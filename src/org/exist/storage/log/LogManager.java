/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-04 The eXist Project
 *  http://exist-db.org
 *  
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *  
 *  $Id$
 */
package org.exist.storage.log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.log4j.Logger;
import org.exist.EXistException;
import org.exist.storage.BrokerPool;
import org.exist.storage.sync.Sync;
import org.exist.storage.txn.Checkpoint;
import org.exist.storage.txn.TransactionException;
import org.exist.util.sanity.SanityCheck;

/**
 * Manages the journalling log. The database uses one central log file for
 * all data files. If the file exceeds the predefined maximum size, a new file is created.
 * Every log file has a unique log number, which keeps growing during the lifetime of the db.
 * The name of the log file is the log file number. The file with the highest log file
 * number will be used for recovery.
 * 
 * A buffer is used to temporarily buffer log entries. To guarantee log consistency, the buffer will be flushed
 * and the log file is synched after every commit or whenever a db page is written to disk.
 * 
 * Each log entry has the structure:
 * 
 * <pre>[byte: entryType, long: transactionId, short length, byte[] data, short backLink]</pre>
 * 
 * <ul>
 *  <li>entryType is a unique id that identifies the log record. Entry types are registered via the 
 * {@link org.exist.storage.log.LogEntryTypes} class.</li>
 *  <li>transactionId: the id of the transaction that created the record.</li>
 *  <li>length: the length of the log entry data.</li>
 *  <li>data: the payload data provided by the {@link org.exist.storage.log.Loggable} object.</li>
 *  <li>backLink: offset to the start of the record. Used when scanning the log file backwards.</li>
 * </ul>
 * 
 * @author wolf
 */
public class LogManager {
    /**
     * Logger for this class
     */
    private static final Logger LOG = Logger.getLogger(LogManager.class);

    public final static String LOG_FILE_SUFFIX = "log";
    public final static String BAK_FILE_SUFFIX = ".bak";
    
    /** the length of the header of each log entry: entryType + transactionId + length */
    public final static int LOG_ENTRY_HEADER_LEN = 11;
	
	/** header length + trailing back link */
    public final static int LOG_ENTRY_BASE_LEN = LOG_ENTRY_HEADER_LEN + 2;
	
    /** default maximum journal size */
    public final static int DEFAULT_MAX_LOG_SIZE = 10 * 1024 * 1024;

    /** minimal size the journal needs to have to be replaced by a new file during a checkpoint */
	private static final long MIN_LOG_REPLACE = 1024 * 1024;
    
    /** 
     * size limit for the log file. A checkpoint will be triggered if the log file
     * exceeds this size limit.
     */
    private int logSizeLimit = DEFAULT_MAX_LOG_SIZE;
    
    /** the current output channel */ 
    private FileChannel channel;
    
    /** Synching the journal is done by a background thread */
    private FileSyncThread syncThread;
    
    /** latch used to synchronize writes to the channel */
    private Object latch = new Object();
    
    /** the data directory where log files are written to */
    private File dir;
    
    /** the current log file number */
    private int currentFile = 0;
    
    /** temp buffer */
    private ByteBuffer currentBuffer;
    
    /** the last LSN written by the LogManager */
    private long currentLsn = Lsn.LSN_INVALID;
    
    /** stores the current LSN of the last file sync on the log file */ 
    private long lastSyncLsn = Lsn.LSN_INVALID;
    
    /** set to true while recovery is in progress */
    private boolean inRecovery = false;
    
    /** the {@link BrokerPool} that created this log manager */
    private BrokerPool pool;
    
    /** if set to true, a sync will be triggered on the log file after every commit */
    private boolean syncOnCommit = true;
    
    public LogManager(BrokerPool pool, File directory) throws EXistException {
        this.dir = directory;
        this.pool = pool;
        currentBuffer = ByteBuffer.allocateDirect(0x40000);
        
        syncThread = new FileSyncThread(latch);
        syncThread.start();
        
        Boolean syncOpt = (Boolean) pool.getConfiguration().getProperty("db-connection.recovery.sync-on-commit");
        if (syncOpt != null) {
        	syncOnCommit = syncOpt.booleanValue();
        	if (LOG.isDebugEnabled())
        		LOG.debug("SyncOnCommit = " + syncOnCommit);
        }
        
        String logDir = (String) pool.getConfiguration().getProperty("db-connection.recovery.journal-dir");
        if (logDir != null) {
            String dbHome = System.getProperty("exist.home");
            File f = new File(logDir);
            if ((!f.isAbsolute()) && dbHome != null) {
                logDir = dbHome + File.separatorChar + logDir;
                f = new File(logDir);
            }
            if (!f.exists()) {
                if (LOG.isDebugEnabled())
                    LOG.debug("Output directory for journal files does not exist. Creating " + f.getAbsolutePath());
                try {
                    f.mkdir();
                } catch (SecurityException e) {
                    throw new EXistException("Failed to create output directory: " + f.getAbsolutePath());
                }
            }
            if (!(f.canWrite())) {
                throw new EXistException("Cannot write to journal output directory: " + f.getAbsolutePath());
            }
            this.dir = f;
        }
        if (LOG.isDebugEnabled())
            LOG.debug("Using directory for the journal: " + dir.getAbsolutePath());
        
        Integer sizeOpt = (Integer) pool.getConfiguration().getProperty("db-connection.recovery.size-limit");
        if (sizeOpt != null)
        	logSizeLimit = sizeOpt.intValue() * 1024 * 1024;
    }
    
    /**
     * Write a log entry to the log.
     * 
     * @param loggable
     * @throws TransactionException
     */
    public synchronized void writeToLog(Loggable loggable) throws TransactionException {
        SanityCheck.ASSERT(!inRecovery, "Write to log during recovery. Should not happen!");
        final int size = loggable.getLogSize();
        final int required = size + LOG_ENTRY_BASE_LEN;
        if (required > currentBuffer.capacity() - currentBuffer.position())
            flushToLog(false);
        try {
            currentLsn = Lsn.create(currentFile, (int) channel.position() + currentBuffer.position() + 1);
            currentBuffer.put(loggable.getLogType());
            currentBuffer.putLong(loggable.getTransactionId());
            currentBuffer.putShort((short) loggable.getLogSize());
            loggable.write(currentBuffer);
            currentBuffer.putShort((short) (size + LOG_ENTRY_HEADER_LEN));
            loggable.setLsn(currentLsn);
        } catch (IOException e) {
            throw new TransactionException("Failed to write to log", e);
        }
    }
    
    /**
     * Flush the current log buffer to disk. If fsync is true, a sync will
     * be called on the file to force all changes to disk.
     * 
     * @param fsync forces all changes to disk if true and syncMode is set to {@link #SYNC_ON_COMMIT}.
     * @throws TransactionException
     */
    public void flushToLog(boolean fsync) {
    	flushToLog(fsync, false);
    }
    
    /**
     * Flush the current log buffer to disk. If fsync is true, a sync will
     * be called on the file to force all changes to disk.
     * 
     * @param fsync forces all changes to disk if true and syncMode is set to {@link #SYNC_ON_COMMIT}.
     * @param forceSync force changes to disk even if syncMode doesn't require it.
     * @throws TransactionException
     */
    public synchronized void flushToLog(boolean fsync, boolean forceSync) {
        if (inRecovery)
            return;
        synchronized (latch) {
            try {
                if (currentBuffer.position() > 0) {
                    currentBuffer.flip();
                    channel.write(currentBuffer);
                    currentBuffer.clear();
                }
            } catch (IOException e) {
                LOG.warn("Flushing log file failed!", e);
            }
        }
        if (forceSync || (fsync && syncOnCommit && currentLsn > lastSyncLsn)) {
            syncThread.triggerSync();
            lastSyncLsn = currentLsn;
        }
        try {
            if (channel.size() >= logSizeLimit)
                pool.triggerCheckpoint();
        } catch (IOException e) {
            LOG.warn("Failed to trigger checkpoint!", e);
        }
    }
    
    /**
     * Write a checkpoint record to the log and flush it. If switchLogFiles is true,
     * a new log file will be started, but only if the log file is larger than
     * {@link #MIN_LOG_REPLACE}. The old log is removed.
     * 
     * @param txnId
     * @param switchLogFiles
     * @throws TransactionException
     */
    public void checkpoint(long txnId, boolean switchLogFiles) throws TransactionException {
    	writeToLog(new Checkpoint(txnId));
		flushToLog(true, true);
        try {
			if (switchLogFiles && channel.position() > MIN_LOG_REPLACE) {
				int last = currentFile;
			    try {
			        switchFiles();
			    } catch (LogException e) {
			        LOG.warn("Failed to create new log file: " + e.getMessage(), e);
			    }
			    File oldFile = getFile(last);
			    oldFile.delete();
			}
		} catch (IOException e) {
			LOG.warn("IOException while writing checkpoint", e);
		}
    }
    
    /**
     * Set the log file number of the last log file used.
     * 
     * @param fileNum the log file number
     */
    public void setCurrentFileNum(int fileNum) {
        currentFile = fileNum;
    }
    
    /**
     * Create a new log file with a larger log file number
     * than the previous file.
     * 
     * @throws LogException
     */
    public void switchFiles() throws LogException {
        ++currentFile;
        String fname = getFileName(currentFile);
        File file = new File(dir, fname);
        if (file.exists()) {
            if (LOG.isDebugEnabled())
                LOG.debug("Log file " + file.getAbsolutePath() + " already exists. Copying it.");
            boolean renamed = file.renameTo(new File(file.getAbsolutePath() + BAK_FILE_SUFFIX));
            if (renamed && LOG.isDebugEnabled())
                LOG.debug("Old file renamed to " + file.getAbsolutePath());
            file = new File(dir, fname);
        }
        if (LOG.isDebugEnabled())
            LOG.debug("Creating new log file: " + file.getAbsolutePath());
        synchronized (latch) {
	        close();
	        try {
				RandomAccessFile raf = new RandomAccessFile(file, "rw");
				channel = raf.getChannel();
	            
	            syncThread.setChannel(channel);
			} catch (FileNotFoundException e) {
				throw new LogException("Failed to open new log file: " + file.getAbsolutePath(), e);
			}
        }
    }
    
    public void close() {
        if (channel != null) {
        	try {
        		channel.close();
        	} catch (IOException e) {
        		LOG.warn("Failed to close journal file", e);
        	}
        }
    }
    
    /**
     * Find the log file with the highest log file number.
     * 
     * @param files
     * @return
     */
	public final static int findLastFile(File files[]) {
		File last = null;
		int max = -1;
		for (int i = 0; i < files.length; i++) {
			int p = files[i].getName().indexOf('.');
			String baseName = files[i].getName().substring(0, p);
			int num = Integer.parseInt(baseName, 16);
			if (num > max) {
				max = num;
				last = files[i];
			}
		}
		return max;
	}

    /**
     * Returns all log files found in the data directory.
     * 
     * @return
     */
	public File[] getFiles() {
		File files[] = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(LOG_FILE_SUFFIX);
			}
		});
		return files;
	}
	
    /**
     * Returns the file corresponding to the specified
     * log file number.
     * 
     * @param fileNum
     * @return
     */
	public File getFile(int fileNum) {
		return new File(dir, getFileName(fileNum));
	}
	
    public void shutdown() {
        syncThread.shutdown();
    }
    
    /**
     * Called to signal that the db is currently in
     * recovery phase, so no log output should be written.
     * 
     * @param value
     */
    public void setInRecovery(boolean value) {
        inRecovery = value;
    }
    
    /**
     * Translate a log file number into a file name.
     * 
     * @param fileNum
     * @return
     */
    private static String getFileName(int fileNum) {
        String hex = Integer.toHexString(fileNum);
        hex = "0000000000".substring(hex.length()) + hex;
        return hex + '.' + LOG_FILE_SUFFIX;
    }
}
