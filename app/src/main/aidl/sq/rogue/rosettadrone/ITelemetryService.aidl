// ITelemetryService.aidl
package sq.rogue.rosettadrone;

// Declare any non-default types here with import statements

interface ITelemetryService {
    boolean start();

    boolean stop();

    boolean restart();

    boolean update();
}
