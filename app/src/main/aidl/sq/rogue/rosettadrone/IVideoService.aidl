// IVideoService.aidl
package sq.rogue.rosettadrone;

// Declare any non-default types here with import statements

interface IVideoService {
    boolean start();

    boolean stop();

    boolean restart();

    boolean update();

}
