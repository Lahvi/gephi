package org.profinit.nodetypefilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JPanel;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.filters.spi.Category;
import org.gephi.filters.spi.CategoryBuilder;
import org.gephi.filters.spi.ComplexFilter;
import org.gephi.filters.spi.Filter;
import org.gephi.filters.spi.FilterBuilder;
import org.gephi.filters.spi.FilterProperty;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.HierarchicalGraph;
import org.gephi.graph.api.Node;
import org.gephi.partition.api.NodePartition;
import org.gephi.partition.api.Part;
import org.gephi.partition.api.Partition;
import org.gephi.partition.api.PartitionController;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 * Pro kazdy sloupec v tabulce uzlu vytvori jeden filtr builder. Tyto vyfiltry 
 * umisti do kategorie Typ uzlu v nabidce filtrovani v UI Gephi. Vyrvoreno
 * podle vzoru v modulu Filter Plugin a podle tutorialu na strankach gephi 
 * @author Lahvi
 */
@ServiceProvider(service = CategoryBuilder.class)
public class NodeTypeCategoryBuilder implements CategoryBuilder {
    /**
     * Nova kategorie "Typ uzlu" v nabidce gephi
     */
    private final static Category NODE_TYPE = new Category("Typ uzlu");

    @Override
    public Category getCategory() {
        return NODE_TYPE;
    }
    /**
     * Pro kazdy sloupce, ktery neni label a ID uzlu vytvori filter buildr
     * @return 
     */
    @Override
    public FilterBuilder[] getBuilders() {
        List<FilterBuilder> builders = new ArrayList<FilterBuilder>();
        PartitionController pc = Lookup.getDefault().lookup(PartitionController.class);
        pc.refreshPartitions();
        NodePartition[] nodePartitions = pc.getModel().getNodePartitions(); //vrati mozne sloupce pro ktere se vytvori filter builder
        for (NodePartition np : nodePartitions) {
            EqualNodeTypeFilterBuilder b = new EqualNodeTypeFilterBuilder(np);
            builders.add(b);
        }
        return builders.toArray(new FilterBuilder[0]); // vraci pole builderu
    }
    /**
     * Vytvori filtr pro jeden urcity atribut uzlu grafu. 
     */
    private static class EqualNodeTypeFilterBuilder implements FilterBuilder {

        private AttributeColumn column;
        private NodePartition partition;
        
        /**
         * @param nodePartition obsahuje dany attribute column, ze ktereho se vyberou
         * moznosti pro filtrovani.
         */
        public EqualNodeTypeFilterBuilder(NodePartition nodePartition) {
            this.partition = nodePartition;
            this.column = nodePartition.getColumn();
        }

        @Override
        public NodeTypeFilter getFilter() {
            return new NodeTypeFilter(partition);
        }

        @Override
        public JPanel getPanel(Filter filter) {
            return new SelectTypePanel((NodeTypeFilter) filter); //mirne upraveny panel z modulu Filter Plugin UI

        }

        @Override
        public Category getCategory() {
            return new Category("Typ uzlu");
        }

        @Override
        public String getName() {
            return column.getTitle() + " filter";
        }

        @Override
        public Icon getIcon() {
            return null;
        }

        @Override
        public String getDescription() {
            return "Filtruje podle vybrane hodnoty z daneho sloupce.";
        }

        @Override
        public void destroy(Filter filter) {
            //No idea what to do here
        }
    }
}
