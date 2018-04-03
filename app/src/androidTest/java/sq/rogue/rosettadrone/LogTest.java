package sq.rogue.rosettadrone;

import org.junit.Before;
import org.junit.Test;

import sq.rogue.rosettadrone.logs.LogFragment;

import static junit.framework.Assert.assertEquals;

public class LogTest {
    private LogFragment log;

    @Before
    public void before() {

        log = new LogFragment();
    }

    @Test
    public void testDefaultLogValue() {
        String logText = log.getLogText();
        assertEquals("", logText);
    }

//    @Test
//    public void testSingleLogAppend() {
//        String textToAppend = "MSG_TXT_APPEND value=0";
//        log.appendLogText(textToAppend);
//
//        assertEquals(textToAppend, log.getLogText());
//    }
//
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
