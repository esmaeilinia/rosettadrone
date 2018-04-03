package sq.rogue.rosettadrone;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import sq.rogue.rosettadrone.logs.LogFragment;

import static junit.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;

public class LogTest {
    private LogFragment log;

    @Before
    public void before() {

        log = Mockito.mock(LogFragment.class);

        Mockito.when(log.getLogText()).thenCallRealMethod();
//        Mockito.doCallRealMethod().when(log).appendLogText(anyString());
    }

    @Test
    public void testDefaultLogValue() {
        String logText = log.getLogText();
        assertEquals("", logText);
    }

    @Test
    public void testClearLog() {
        log.setLogText("TESTING");
        log.clearLogText();
        assertEquals("", log.getLogText());
    }



//    @Test
//    public void testSingleLogAppend() {
//        String textToAppend = "MSG_TXT_APPEND value=0";
//        log.appendLogText(textToAppend);
//        assertEquals(textToAppend, log.getLogText());
//    }

//    @Test
//    public void testMultipleLogAppend() {
//        String textToAppend = "MSG_TXT_APPEND value=hallo";
//
//        log.appendLogText(textToAppend);
//        log.appendLogText(textToAppend);
//        log.appendLogText(textToAppend);
//
//        String verificationString = textToAppend + "\n" + textToAppend + "\n" + textToAppend;
//
//        assertEquals(verificationString, log.getLogText());
//    }
}
