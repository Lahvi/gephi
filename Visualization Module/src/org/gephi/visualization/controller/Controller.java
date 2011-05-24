/*
Copyright 2008-2011 Gephi
Authors : Antonio Patriarca <antoniopatriarca@gmail.com>
Website : http://www.gephi.org

This file is part of Gephi.

Gephi is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

Gephi is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with Gephi.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.gephi.visualization.controller;


import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import org.gephi.lib.gleem.linalg.Vec3f;
import org.gephi.visualization.camera.Camera;
import org.gephi.visualization.geometry.AABB;
import org.gephi.visualization.view.View;
import org.openide.util.Lookup;

/**
 *
 * @author Antonio Patriarca <antoniopatriarca@gmail.com>
 */
public class Controller implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {

    private Camera camera;
    private MotionManager3D motionManager;

    private static Controller instance;

    private boolean centered;

    private Controller() {
        // Random values
        this.camera = new Camera(300, 300, 100f, 10000.0f);
        this.motionManager = new MotionManager3D();
    }

    public synchronized static Controller getInstance() {
        if (instance == null) {
            instance = new Controller();
        }
        return instance;
    }

    public void resize(int width, int height) {
        this.camera.setImageSize(width, height);
    }

    public Camera getCamera() {
        return this.camera;

    }

    public void beginUpdateFrame() {
    }

    public void endUpdateFrame() {
    }

    public void beginRenderFrame() {
    }

    public void endRenderFrame() {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        motionManager.mousePressed(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        motionManager.mouseReleased(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        motionManager.mouseDragged(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        motionManager.mouseMoved(e);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        motionManager.mouseWheelMoved(e);
    }

    public void centerCamera(AABB box) {
        if (centered || box == null) {
            return;
        }

        final Vec3f center = box.center();
        final Vec3f scale = box.scale();
        final Vec3f minVec = box.minVec();
        final Vec3f maxVec = box.maxVec();

        float d = scale.y() / (float)Math.tan(0.5 * camera.fov());

        final Vec3f origin = new Vec3f(center.x(), center.y(), minVec.z() - d*1.1f);
        camera.lookAt(origin, center, Vec3f.Y_AXIS);
        //camera.setClipPlanes(d, maxVec.z() - minVec.z() + d*1.2f);
        centered = true;
    }

    // TODO - create a better architecture containing class or leave here?
    public MotionManager3D getMotionManager() {
        return motionManager;
    }

}