package  trendmicro.com.tangoindoornavigation;

import org.rajawali3d.scene.ASceneFrameCallback;

/**
 * Created by hugo on 24/07/2017.
 */
public abstract class ScenePreFrameCallbackAdapter extends ASceneFrameCallback {


    @Override
    public void onPreDraw(long sceneTime, double deltaTime) {

    }

    @Override
    public void onPostFrame(long sceneTime, double deltaTime) {

    }

    @Override
    public boolean callPreFrame() {
        return true;
    }

}
