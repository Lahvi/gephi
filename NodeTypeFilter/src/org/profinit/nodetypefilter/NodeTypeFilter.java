package org.profinit.nodetypefilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.filters.spi.ComplexFilter;
import org.gephi.filters.spi.FilterProperty;
import org.gephi.filters.spi.NodeFilter;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.HierarchicalGraph;
import org.gephi.graph.api.Node;
import org.gephi.partition.api.Part;
import org.gephi.partition.api.Partition;
import org.gephi.partition.api.PartitionController;
import org.openide.util.Lookup;

/**
 * Vyfiltruje uzly grafu podle vybrane hodnoty (i vice) z jednoho vybraneho
 * sloupce. V grafu ponecha vsechny uzly ktery maji dany atribut odpovidajici
 * vybranemu sloupci stejny jako vybrana hodnota.
 * 
 * @author Lahvi
 */
public class NodeTypeFilter implements ComplexFilter {

    protected Partition partition;
    protected FilterProperty[] filterProperties;
    protected List<Part> parts; //seznam vsech nestenych hodnot ve slouci

    /**
     * @param partition seznam vsech nestejnych hodnot v danem attribute
     * sloupci, ze kterych si uzivatel vybere podle jako bude filtrovat
     */
    public NodeTypeFilter(Partition partition) {
        this.partition = partition;
        parts = new ArrayList<Part>();
    }

    public boolean init(Graph graph) {
        HierarchicalGraph hg = (HierarchicalGraph) graph;
        //parition obsahuje mnozinu hodnot ktere se ve sloupci vyskyutji (napriklad pro sloupec pohlavi zde bude muz, zena)
        this.partition = Lookup.getDefault().lookup(PartitionController.class).buildPartition(partition.getColumn(), hg);
        if (partition.getParts().length > 0) {
            return true;
        }
        return false;
    }

    public void finish() {
    }

    /**
     * Vyfiltruje uzly grafu podle vybrane hodnoty (i vice) z jednoho vybraneho
     * sloupce. V grafu ponecha vsechny uzly ktery maji dany atribut
     * odpovidajici vybranemu sloupci stejny jako vybrana hodnota.
     *
     * @param graph graf, ktery se bude filtrovat
     * @return vyfiltrovany graf
     */
    @Override
    public Graph filter(Graph graph) {
        int graphViewID = graph.getView().getViewId(); //ID view filtrovaneho grafu
        Graph mainGraph = graph.getGraphModel().getGraph(); //nefiltrovany graf s puvodnim view
        Node[] graphNodes = mainGraph.getNodes().toArray();

        for (Node node : graphNodes) {
            boolean remove = true;
            Object value = node.getNodeData().getAttributes().getValue(partition.getColumn().getIndex());
            int size = parts.size();
            for (int i = 0; i < size; i++) {
                Object obj = parts.get(i).getValue();
                if (obj.equals(value)) {
                    remove = false; //nema byt odstranen, alespon jeden parametr ma shodu
                }
            }
            if (remove) {
                Node removedNode = node.getNodeData().getNode(graphViewID); //uzel ve view filtrovaneho grafu
                if (removedNode != null) {
                    graph.removeNode(node); //odstranuji uzel z filtrovaneho grafu
                }
            }
        }
        //vytvoreni hran mezi vyfiltrovanymi uzly podle cest v nefiltrovanem grafu
        graph = new FilterEdgesFactory(mainGraph, graph).createHidenEdges();
        return graph;
    }

    @Override
    public String getName() {
        return partition.getColumn().getTitle() + " filter";
    }

    /**
     * Pridava novou hodnotu ve sloupci do moznosti pro vyber hodnoty, podle
     * ktere se bude filtrovat
     *
     * @param part
     */
    public void addPart(Part part) {
        if (!parts.contains(part)) {
            List<Part> newParts = new ArrayList<Part>(parts.size() + 1);
            newParts.addAll(parts);
            newParts.add(part);
            getProperties()[1].setValue(newParts);
        }
    }

    /**
     * Odebira to same jako addPart
     *
     * @param part
     */
    public void removePart(Part part) {
        List<Part> newParts = new ArrayList<Part>(parts);
        if (newParts.remove(part)) {
            getProperties()[1].setValue(newParts);
        }
    }

    /**
     * Odstrani vsechny vybrane hodnoty podle kterych se melo filtrovat
     */
    public void unselectAll() {
        getProperties()[1].setValue(new ArrayList<Part>());
    }

    /**
     * Do vybranych hodnot, podle kterych se bude filtrovat se pridaji vsechny
     * moznosti
     */
    public void selectAll() {
        getProperties()[1].setValue(Arrays.asList(partition.getParts()));
    }

    /**
     *
     * @return
     */
    @Override
    public FilterProperty[] getProperties() {
        if (filterProperties == null) {
            filterProperties = new FilterProperty[0];
            try {
                filterProperties = new FilterProperty[]{
                    FilterProperty.createProperty(this, AttributeColumn.class, "column"),
                    FilterProperty.createProperty(this, List.class, "parts")};
            } catch (Exception ex) {
                //TODO: nejake logovani by to chtelo
            }
        }
        return filterProperties;
    }

    public Partition getCurrentPartition() {
        if (partition.getPartsCount() == 0) {
            //build partition
            GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
            this.partition = Lookup.getDefault().lookup(PartitionController.class).buildPartition(partition.getColumn(), graphModel.getHierarchicalGraphVisible());
        }
        return partition;
    }

    public Partition getPartition() {
        return partition;
    }

    public List<Part> getParts() {
        return parts;
    }

    public AttributeColumn getColumn() {
        return partition.getColumn();
    }

    public void setColumn(AttributeColumn column) {
    }

    public void setParts(List<Part> parts) {
        this.parts = parts;
    }

    public void setPartition(Partition partition) {
        this.partition = partition;
    }
}
