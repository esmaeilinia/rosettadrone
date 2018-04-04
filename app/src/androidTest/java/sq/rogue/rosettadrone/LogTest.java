package sq.rogue.rosettadrone;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.w3c.dom.Text;

import sq.rogue.rosettadrone.logs.LogFragment;

import static junit.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

public class LogTest {
    private LogFragment log;
    private Context context;

    @Before
    public void before() {
        context = InstrumentationRegistry.getTargetContext();


        log = Mockito.mock(LogFragment.class);

        Mockito.when(log.getLogText()).thenCallRealMethod();
        Mockito.doCallRealMethod().when(log).appendLogText(anyString());
        Mockito.doCallRealMethod().when(log).setTextView(any(TextView.class));
        log.setTextView(new TextView(context));

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



    @Test
    public void testSingleLogAppend() {
        String textToAppend = "MSG_TXT_APPEND value=0";
        log.appendLogText(textToAppend);
        assertEquals(textToAppend, log.getLogText());
    }

    @Test
    public void testMultipleLogAppend() {
        String textToAppend = "MSG_TXT_APPEND value=hallo";

        log.appendLogText(textToAppend);
        log.appendLogText(textToAppend);
        log.appendLogText(textToAppend);

        String verificationString = textToAppend + textToAppend + textToAppend;

        assertEquals(verificationString, log.getLogText());
    }
}
