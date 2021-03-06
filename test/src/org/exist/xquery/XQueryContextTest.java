package org.exist.xquery;

import org.exist.storage.DBBroker;
import org.exist.security.Subject;
import org.junit.Test;
import org.easymock.EasyMock;
import org.exist.security.xacml.AccessContext;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.easymock.EasyMock.expect;
/**
 *
 * @author aretter
 */
public class XQueryContextTest {

    @Test
    public void prepareForExecution_setsUserFromSession() {

        //partial mock context
        XQueryContext context = EasyMock.createMockBuilder(XQueryContext.class)
                .withConstructor(AccessContext.class)
                .withArgs(AccessContext.TEST)
                .addMockedMethod("getUserFromHttpSession")
                .addMockedMethod("getBroker")
                .createMock();

        DBBroker mockBroker = EasyMock.createMock(DBBroker.class);

        Subject mockSubject = EasyMock.createMock(Subject.class);

        //expectations
        expect(context.getUserFromHttpSession()).andReturn(mockSubject);
        expect(context.getBroker()).andReturn(mockBroker).times(2);
        mockBroker.pushSubject(mockSubject);

        //test
        replay(context);

        context.prepareForExecution();

        verify(context);
    }
}