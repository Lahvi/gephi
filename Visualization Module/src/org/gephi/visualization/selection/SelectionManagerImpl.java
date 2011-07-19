/*
Copyright 2008-2011 Gephi
Authors : Vojtech Bardiovsky <vojtech.bardiovsky@gmail.com>
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

package org.gephi.visualization.selection;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphEvent;
import org.gephi.graph.api.GraphListener;
import org.gephi.graph.api.Node;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.project.api.WorkspaceListener;
import org.gephi.visualization.api.controller.MotionManager;
import org.gephi.visualization.api.config.VizConfig;
import org.gephi.visualization.api.selection.NodeSpatialStructure;
import org.gephi.visualization.api.selection.SelectionManager;
import org.gephi.visualization.api.selection.SelectionType;
import org.gephi.visualization.api.selection.Shape;
import org.gephi.visualization.api.vizmodel.VizModel;
import org.gephi.visualization.apiimpl.shape.ShapeUtils;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = SelectionManager.class)
public class SelectionManagerImpl implements SelectionManager, WorkspaceListener, GraphListener {

    private NodeSpatialStructure nodeContainer;

    private boolean blocked;

    private int mouseSelectionDiameter;
    private boolean mouseSelectionZoomProportional;

    private Shape singleNodeSelectionShape;

    private List<ChangeListener> listeners = new ArrayList<ChangeListener>();

    @Override
    public void initialize() {
        Lookup.getDefault().lookup(GraphController.class).getModel().addGraphListener(this);
        Lookup.getDefault().lookup(ProjectController.class).addWorkspaceListener(this);
        VizConfig vizConfig = Lookup.getDefault().lookup(VizConfig.class);
        mouseSelectionDiameter = vizConfig.getMouseSelectionDiameter();
        mouseSelectionZoomProportional = vizConfig.isMouseSelectionZoomProportionnal();
    }

    @Override
    public Collection<Node> getSelectedNodes() {
        return nodeContainer.getSelectedNodes();
    }

    @Override
    public void applySelection(Shape shape) {
        nodeContainer.applySelection(shape);
    }

    @Override
    public void applyContinuousSelection(Shape shape) {
        nodeContainer.applyContinuousSelection(shape);
    }

    @Override
    public void cancelContinuousSelection() {
        nodeContainer.clearContinuousSelection();
    }

    @Override
    public void clearSelection() {
        nodeContainer.clearSelection();
    }

    @Override
    public void selectSingle(Point point, boolean select) {
        singleNodeSelectionShape = ShapeUtils.createEllipseShape(point.x - mouseSelectionDiameter, point.y - mouseSelectionDiameter, mouseSelectionDiameter, mouseSelectionDiameter);
        nodeContainer.clearContinuousSelection();
        nodeContainer.selectSingle(singleNodeSelectionShape, point, select, NodeSpatialStructure.SINGLE_NODE_CLOSEST);
    }

    @Override
    public boolean selectContinuousSingle(Point point, boolean select) {
        singleNodeSelectionShape = ShapeUtils.createEllipseShape(point.x - mouseSelectionDiameter, point.y - mouseSelectionDiameter, mouseSelectionDiameter, mouseSelectionDiameter);
        return nodeContainer.selectContinuousSingle(singleNodeSelectionShape, point, select, NodeSpatialStructure.SINGLE_NODE_CLOSEST);
    }

    @Override
    public void refreshDataStructure() {
        boolean use3d = Lookup.getDefault().lookup(VizModel.class).isUse3d();
        GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
        if (use3d) {
            nodeContainer = new Octree(graphController.getModel().getGraph());
        } else {
            nodeContainer = new Quadtree(graphController.getModel().getGraph());
        }
    }

    @Override
    public void blockSelection(boolean block) {
        // TODO implement reasonable block for tools
        VizConfig vizConfig = Lookup.getDefault().lookup(VizConfig.class);
        if (vizConfig.getSelectionType() != SelectionType.NONE) {
            this.blocked = block;
            vizConfig.setSelectionEnable(!block);
            fireChangeEvent();
        } else {
            setDirectMouseSelection();
        }
    }

    @Override
    public void disableSelection() {
        // move to NodeSpatialStructure and clear marker
        for (Node node : nodeContainer.getSelectedNodes()) {
            node.getNodeData().setSelected(false);
        }
        nodeContainer.clearCache();
        Lookup.getDefault().lookup(VizConfig.class).setSelectionEnable(false);
    }

    @Override
    public int getMouseSelectionDiameter() {
        return mouseSelectionDiameter;
    }

    @Override
    public Shape getNodePointerShape() {
        MotionManager motionManager = Lookup.getDefault().lookup(MotionManager.class);
        singleNodeSelectionShape = ShapeUtils.createEllipseShape(motionManager.getMousePosition()[0] - mouseSelectionDiameter, 
                                                                 motionManager.getMousePosition()[1] - mouseSelectionDiameter,
                                                                 mouseSelectionDiameter, mouseSelectionDiameter);
        return (isDirectMouseSelection() || isDraggingEnabled()) && motionManager.isInside() && !motionManager.isPressing() && mouseSelectionDiameter > 1 ? singleNodeSelectionShape : null;
    }

    @Override
    public boolean isBlocked() {
        return blocked;
    }

    @Override
    public boolean isDirectMouseSelection() {
        return Lookup.getDefault().lookup(VizConfig.class).isDirectMouseSelection();
    }

    @Override
    public boolean isDraggingEnabled() {
        return Lookup.getDefault().lookup(VizConfig.class).isDraggingEnabled();
    }

    @Override
    public boolean isMouseSelectionZoomProportional() {
        return mouseSelectionZoomProportional;
    }

    @Override
    public boolean isSelectionEnabled() {
        return Lookup.getDefault().lookup(VizConfig.class).isSelectionEnabled();
    }

    @Override
    public SelectionType getSelectionType() {
        VizConfig vizConfig = Lookup.getDefault().lookup(VizConfig.class);
        return vizConfig.getSelectionType();
    }

    @Override
    public boolean isMovementEnabled() {
        return Lookup.getDefault().lookup(VizConfig.class).isMovementEnabled();
    }

    @Override
    public void removeChangeListener(ChangeListener changeListener) {
        listeners.remove(changeListener);
    }

    @Override
    public void selectEdge(Edge edge) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void selectEdges(Edge[] edges) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void selectNode(Node node) {
        // move to NodeSpatialStructure and clear marker
        node.getNodeData().setSelected(true);
    }

    @Override
    public void selectNodes(Node[] nodes) {
        // move to NodeSpatialStructure and clear marker
        for (Node node : nodes) {
            node.getNodeData().setSelected(true);
        }
    }

    @Override
    public void setDraggingEnable(boolean dragging) {
        clearState();
        VizConfig vizConfig = Lookup.getDefault().lookup(VizConfig.class);
        vizConfig.setDraggingEnable(true);
        fireChangeEvent();
    }

    @Override
    public void setSelectionType(SelectionType selectionType) {
        clearState();
        VizConfig vizConfig = Lookup.getDefault().lookup(VizConfig.class);
        vizConfig.setSelectionType(selectionType);
        fireChangeEvent();
    }

    @Override
    public void setDirectMouseSelection() {
        clearState();
        VizConfig vizConfig = Lookup.getDefault().lookup(VizConfig.class);
        vizConfig.setDirectMouseSelection(true);
        fireChangeEvent();
    }

    @Override
    public void setMovementEnabled(boolean enabled) {
        clearState();
        VizConfig vizConfig = Lookup.getDefault().lookup(VizConfig.class);
        vizConfig.setMovementEnabled(true);
        fireChangeEvent();
    }

    private void clearState() {
        VizConfig vizConfig = Lookup.getDefault().lookup(VizConfig.class);
        vizConfig.setDraggingEnable(false);
        vizConfig.setSelectionType(SelectionType.NONE);
        vizConfig.setMovementEnabled(false);
        vizConfig.setDirectMouseSelection(false);
        this.blocked = false;
    }

    @Override
    public void setMouseSelectionDiameter(int mouseSelectionDiameter) {
        this.mouseSelectionDiameter = mouseSelectionDiameter;
    }

    @Override
    public void setMouseSelectionZoomProportionnal(boolean mouseSelectionZoomProportionnal) {
        this.mouseSelectionZoomProportional = mouseSelectionZoomProportionnal;
    }

    @Override
    public void addChangeListener(ChangeListener changeListener) {
        listeners.add(changeListener);
    }

    private void fireChangeEvent() {
        ChangeEvent evt = new ChangeEvent(this);
        for (ChangeListener l : listeners) {
            l.stateChanged(evt);
        }
    }

    // Graph event
    @Override
    public void graphChanged(GraphEvent event) {
        if (nodeContainer == null) {
            refreshDataStructure();
        }
        switch (event.getEventType()) {
            case ADD_NODES:
                for (Node node : event.getData().addedNodes()) {
                    nodeContainer.addNode(node);
                }
                break;
            default:
                refreshDataStructure();
                break;
        }
    }

    // Workspace event
    @Override
    public void initialize(Workspace workspace) {
    }

    @Override
    public void select(Workspace workspace) {
        GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
        nodeContainer = workspace.getLookup().lookup(NodeSpatialStructure.class);
        if (nodeContainer == null) {
            nodeContainer = new Octree(graphController.getModel().getGraph());
        }
    }

    @Override
    public void unselect(Workspace workspace) {
    }

    @Override
    public void close(Workspace workspace) {
    }

    @Override
    public void disable() {
    }

}