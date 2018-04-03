package sq.rogue.rosettadrone;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.test.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import sq.rogue.rosettadrone.settings.SettingsFragment;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;

public class PreferencesTest {

    SettingsFragment settingsFragment;

    @Before
    public void before() {
        settingsFragment = Mockito.mock(SettingsFragment.class);
        Mockito.when(settingsFragment.validate_ip(anyString())).thenCallRealMethod();
    }

    @Test
    public void validIPTest() {
        System.out.println(settingsFragment.validate_ip("192.168.0.1"));
        assertTrue(settingsFragment.validate_ip("127.0.0.1"));
    }

    @Test
    public void invalidIPTest() {
        assertFalse(settingsFragment.validate_ip("266.212.231.020"));
    }

    @Test
    public void invalidShortIPTest() {
        assertFalse(settingsFragment.validate_ip("127.0.1"));
    }

    @Test
    public void invalidLongIPTest() {
        assertFalse(settingsFragment.validate_ip("127.0.0.1.1"));
    }

    @Test
    public void invalidZeroIPTest() {
        assertFalse(settingsFragment.validate_ip("0.0.0.0"));
    }
}
