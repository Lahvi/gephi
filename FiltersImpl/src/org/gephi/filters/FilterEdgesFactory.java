/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.gephi.filters;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphFactory;
import org.gephi.graph.api.Node;

/**
 * The <code>FilteredEdgesFactory</code> class contains the logic for creating
 * hidden edges. User can call just one method {@link FilterEdgesFactory#createHidenEdges() }.
 * Hidden edge is that edge which leads through unfiltered nodes from one 
 * - source - filtered node to next - target - node.
 * @author Lahvi
 */
public class FilterEdgesFactory {

    private Set<Node> visitedNodes;
    private Graph graph;
    private Graph filteredGraph;
    private Queue<NodePathWrapper> wrapperQueue;
    private Set<Edge> filtredGraphEdges;

    public FilterEdgesFactory(Graph graph, Graph filteredGraph) {
        this.graph = graph;
        this.filteredGraph = filteredGraph;
        visitedNodes = new HashSet<Node>();
        wrapperQueue = new LinkedList<NodePathWrapper>();
        setFiltredEdges();
    }

    /**
     * Metoda prochází nefiltrovaný graf do šířky, vždy se začíná expandovat z
     * vyfiltrovaného uzlu. Pokud při procházení metoda nalezne vyfiltrovaný
     * uzel tak zkotroluje jestli mezi uzly existuje hrana, pokud ano tak
     * porovná
     *
     * @return
     */
    public Graph createHidenEdges() {
        //projdu vsechny vyfiltrovane uzly
        Node[] filteredNodes = filteredGraph.getNodes().toArray();
        for (int i = 0; i < filteredNodes.length; i++) {
            findHidenEdges(filteredNodes[i]);
        }
        return filteredGraph;
    }

    private void findHidenEdges(Node sourceNode) {
        clearCollections();
        GraphFactory edgesFactory = filteredGraph.getGraphModel().factory();
        //prvotni expandovani vychoziho uzlu - expanduji pouze nevyfiltrovane uzly
        Node[] nodeNeigh = graph.getNeighbors(sourceNode).toArray();
        for (int i = 0; i < nodeNeigh.length; i++) {
            if (!filteredGraph.contains(nodeNeigh[i])) {
                wrapperQueue.add(new NodePathWrapper(nodeNeigh[i]));
            } else {
                visitedNodes.add(nodeNeigh[i]);
            }
        }
        visitedNodes.add(sourceNode);
        while (!wrapperQueue.isEmpty()) {
            NodePathWrapper actualWrapper = wrapperQueue.remove();
            //pokud je prvni uzel ve fronte vyfiltrovany a neexistuje hrana
            if (filteredGraph.contains(actualWrapper.lastNode)) {
                if (!filteredGraph.isAdjacent(sourceNode, actualWrapper.lastNode)) {
                    //nova hrana
                    filteredGraph.addEdge(edgesFactory.newEdge(sourceNode, actualWrapper.lastNode, actualWrapper.totalCost, false));
                } else {
                    Edge existingEdge = filteredGraph.getEdge(sourceNode, actualWrapper.lastNode);
                    if (!filtredGraphEdges.contains(existingEdge)) { //pokud je hrana v puvodnim vyfiltrovanem grafu tak ji nebudu nahrazovat levnejsi cestou
                        if (existingEdge.getWeight() > actualWrapper.totalCost) {
                            existingEdge.setWeight(actualWrapper.totalCost);
                        }
                    }
                }

            } else {
                //pokud neni vyfiltrovany pokracuji v expanzi
                expandNodes(actualWrapper);
            }
            //uzel je zpracovan
            visitedNodes.add(actualWrapper.lastNode);
        }
    }
    //Expanduje uzel - zjisti vsechny jeho sousedy, expanduje jejich wrappery
    //a ty ulozi do fronty, pokud jiz uzel neni oznacen jako navstiveny
    private void expandNodes(NodePathWrapper expandedNode) {
        Node[] neighboroughs = graph.getNeighbors(expandedNode.lastNode).toArray();
        for (int i = 0; i < neighboroughs.length; i++) {
            if (!visitedNodes.contains(neighboroughs[i])) {
                float edgeWeight = graph.getEdge(expandedNode.lastNode, neighboroughs[i]).getWeight();
                wrapperQueue.add(expandedNode.expandWrapper(neighboroughs[i], edgeWeight));
            }
        }
    }

    private void clearCollections() {
        wrapperQueue.clear();
        visitedNodes.clear();
    }
    //ulozi vsechny hrany, ktere obsahuje vyfiltrovany graf na zacatku.
    private void setFiltredEdges() {
        filtredGraphEdges = new HashSet<Edge>();
        Edge[] filtredEdges = filteredGraph.getEdges().toArray();
        filtredGraphEdges.addAll(Arrays.asList(filtredEdges));
    }

    /**
     * Trida obaluje dva uzly (vychozi a cilovy) a cenu, kterou stalo dostat se
     * od vychoziho uzle do ciloveho
     */
    private class NodePathWrapper {

        private Node sourceNode;
        private Node lastNode;
        private float totalCost;

        private NodePathWrapper(Node sourceNode) {
            this.sourceNode = sourceNode;
            this.lastNode = sourceNode;
            totalCost = 0f;
        }
        //nastavi novy lastNode a zvysi cenu to kolik stalo se dostat z predchoziho
        //lastNodu do noveho
        private void addNode(Node nextNode, float cost) {
            lastNode = nextNode;
            totalCost += cost;
        }

        /**
         * Metoda vytvori klon a nastavi novy cilovy uzel a zvysi cenu.
         *
         * @param nextNode novy cilovy uzel.
         * @param cost cena, kterou stoji dostat se od posledniho ciloveho uzlz
         * do noveho uzly.
         * @return nova instance wrapperu.
         */
        private NodePathWrapper expandWrapper(Node nextNode, float cost) {
            NodePathWrapper clone = new NodePathWrapper(sourceNode);
            clone.totalCost = totalCost;
            clone.addNode(nextNode, cost);
            return clone;
        }
    }
}
