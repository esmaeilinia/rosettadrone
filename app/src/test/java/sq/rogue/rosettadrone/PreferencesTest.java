package sq.rogue.rosettadrone;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import sq.rogue.rosettadrone.settings.SettingsFragment;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

public class PreferencesTest {

    private SettingsFragment settingsFragment;
//    private SharedPreferences sharedPreferences;

    @Before
    public void before() {
        settingsFragment = Mockito.mock(SettingsFragment.class);
        Mockito.when(settingsFragment.validate_port(anyInt())).thenCallRealMethod();

//        sharedPreferences = Mockito.mock(SharedPreferences.class);
    }

    @Test
    public void validLowPortTest() {
        assertTrue(settingsFragment.validate_port(1));
    }

    @Test
    public void validHighPortTest() {
        assertTrue(settingsFragment.validate_port(65535));
    }

    @Test
    public void invalidHighPortTest() {
        assertFalse(settingsFragment.validate_port(65536));
    }
    @Test
    public void invalidLowPortTest() {
        assertFalse(settingsFragment.validate_port(0));
    }


}
