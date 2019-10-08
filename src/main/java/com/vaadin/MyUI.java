package com.vaadin;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.data.Item;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.*;
import org.apache.commons.collections.MapUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This UI is the application entry point. A UI may either represent a browser window 
 * (or tab) or some part of a html page where a Vaadin application is embedded.
 * <p>
 * The UI is initialized using {@link #init(VaadinRequest)}. This method is intended to be 
 * overridden to add component to the user interface and initialize non-component functionality.
 */
@Theme("mytheme")
public class MyUI extends UI {


    class Node<T> {
        T data;
        Set<Node<T>> children = new HashSet<>();

        public Node(T data) {
            this.data = data;
        }

        public void addChild(Node node) {
            children.add(node);
        }

        public Collection<Node<T>> getChildren() {
            return children;
        }

        public boolean hasChildren() {
            return !children.isEmpty();
        }
    }


    class Tree<T> {

        Map<T, Node<T>> nodeMap = new HashMap<>();

        public boolean hasChildren(T item) {
            return nodeMap.get(item).hasChildren();
        }

        public Collection<T> getChildren(T item) {
            Collection<Node<T>> children = nodeMap.get(item).getChildren();
            return children.stream().map(node->node.data).collect(Collectors.toList());
        }


        Set<Node<T>> roots = new HashSet<>();

        public Tree(Set<Node<T>> roots) {
            this.roots.addAll(roots);
            Queue<Node<T>> queue = new LinkedList();
            queue.addAll(roots);

            while (!queue.isEmpty()) {
                Node<T> poll = queue.poll();
                nodeMap.put(poll.data, poll);
                queue.addAll(poll.getChildren());
            }
        }

        public void addChild(T row, T not_root) {
            Node node = new Node(not_root);
            nodeMap.get(row).addChild(node);
            nodeMap.put(not_root, node);
        }
    }

    public class Row {
        String first;
        String second;

        public String getFirst() {
            return first;
        }

        public String getSecond() {
            return second;
        }

        public Row(String first, String second) {
            this.first = first;
            this.second = second;
        }
    }

    Map<Object, Row> idToRow = new HashMap<>();


    @Override
    protected void init(VaadinRequest vaadinRequest) {

        final VerticalLayout layout = new VerticalLayout();

        Tree<Row> tree = createTree();

        Grid grid = createGrid(tree, tree.roots.stream().map(node -> node.data).toArray(size -> new Row[size]));

        for (Node<Row> node : tree.roots) {
            Object itemId = grid.addRow(node.data.first, node.data.second);
            idToRow.put(itemId, node.data);
        }

        layout.addComponents(grid);
        layout.setMargin(true);
        layout.setSpacing(true);
        
        setContent(layout);
    }

    Grid createGrid(Tree<Row> rowTree, Row ... roots) {
        Grid.DetailsGenerator detailsGen = (Grid.DetailsGenerator) rowReference -> {
            Row item = idToRow.get(rowReference.getItemId());
            if (rowTree.hasChildren(item)) {
                Grid grid = createGrid(rowTree, item);
                HorizontalLayout horizontalLayout = new HorizontalLayout();
                horizontalLayout.addComponent(new Label("+"));
                horizontalLayout.addComponent(grid);
                return horizontalLayout;
            } else return null;
        };

        Grid grid = new Grid();
        grid.addColumn("first");
        grid.addColumn("second");
        grid.setDetailsGenerator(detailsGen);
        grid.addItemClickListener((ItemClickEvent.ItemClickListener) itemClickEvent -> {
            if (itemClickEvent.isDoubleClick()) {
                grid.setDetailsVisible(itemClickEvent.getItemId(), !grid.isDetailsVisible(itemClickEvent.getItemId()));
            }
        });
        for (Row row : roots) {
            Object itemId = grid.addRow(row.first, row.second);
            idToRow.put(itemId, row);
            grid.setDetailsVisible(itemId, true);
        }

        return grid;
    }

    Grid createGrid(Tree<Row> rowTree, Row row) {
        Grid.DetailsGenerator detailsGen = (Grid.DetailsGenerator) rowReference -> {
            Row item = idToRow.get(rowReference.getItemId());
            if (rowTree.hasChildren(item)) {
                Grid grid = createGrid(rowTree, item);
                HorizontalLayout horizontalLayout = new HorizontalLayout();
                horizontalLayout.addComponent(new Label("+"));
                horizontalLayout.addComponent(grid);
                return horizontalLayout;
            } else return null;
        };


        Grid grid = new Grid();
        grid.addColumn("first");
        grid.addColumn("second");
        grid.setDetailsGenerator(detailsGen);
        grid.addItemClickListener((ItemClickEvent.ItemClickListener) itemClickEvent -> {
            if (itemClickEvent.isDoubleClick()) {
                grid.setDetailsVisible(itemClickEvent.getItemId(), !grid.isDetailsVisible(itemClickEvent.getItemId()));
            }
        });

        rowTree.getChildren(row).forEach(r-> {
            Object itemId = grid.addRow(r.first, r.second);
            grid.setDetailsVisible(itemId, true);
        });
        return grid;
    }


    private Tree<Row> createTree() {
        Row row = new Row("Root", "1");
        Row row2 = new Row("Root", "2");
        Row row3 = new Row("Root", "3");

        HashSet<Node<Row>> roots = new HashSet<>();
        roots.add(new Node<>(row));
        roots.add(new Node<>(row2));
        roots.add(new Node<>(row3));

        Tree<Row> rowTree = new Tree<>(roots);
        Row not_root2 = new Row("Not root", "4");
        rowTree.addChild(row, not_root2);
        rowTree.addChild(row, new Row("Not root", "5"));

        rowTree.addChild(not_root2, new Row("QWEASD", "55"));
        rowTree.addChild(not_root2, new Row("ASDZXC", "66"));
        rowTree.addChild(not_root2, new Row("ZXCASD", "77"));

        Row not_root = new Row("Not root", "6");
        Row not_root1 = new Row("Not root", "7");
        rowTree.addChild(row2, not_root);
        rowTree.addChild(row2, not_root1);

        rowTree.addChild(not_root, new Row("Not root", "8"));
        rowTree.addChild(not_root1, new Row("Not root", "9"));
        return rowTree;
    }

    @WebServlet(urlPatterns = "/*", name = "MyUIServlet", asyncSupported = true)
    @VaadinServletConfiguration(ui = MyUI.class, productionMode = false)
    public static class MyUIServlet extends VaadinServlet {
    }
}
