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

import java.io.Serializable;
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


    Map<Object, Row> idToRow = new HashMap<>();

    class Node<T> implements Serializable{
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node<?> node = (Node<?>) o;
            return data.equals(node.data);
        }

        @Override
        public int hashCode() {
            return Objects.hash(data);
        }
    }

    class Tree<T> implements Serializable {

        Map<T, Node<T>> nodeMap = new HashMap<>();
        Set<Node<T>> roots = new HashSet<>();

        public boolean hasChildren(T item) {
            return nodeMap.get(item).hasChildren();
        }

        public Collection<T> getChildren(T item) {
            Collection<Node<T>> children = nodeMap.get(item).getChildren();
            return children.stream().map(node -> node.data).collect(Collectors.toList());
        }

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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tree<?> tree = (Tree<?>) o;
            return Objects.equals(nodeMap, tree.nodeMap) &&
                    Objects.equals(roots, tree.roots);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nodeMap, roots);
        }
    }

    public class Row {
        String first;
        Integer second;

        public String getFirst() {
            return first;
        }

        public Integer getSecond() {
            return second;
        }

        public Row(String first, Integer second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Row row = (Row) o;
            return Objects.equals(first, row.first) &&
                    Objects.equals(second, row.second);
        }

        @Override
        public int hashCode() {
            return Objects.hash(first, second);
        }
    }


    @Override
    protected void init(VaadinRequest vaadinRequest) {

        final VerticalLayout layout = new VerticalLayout();

        Tree<Row> tree = createTree();

        Grid grid = createGrid(tree, tree.roots.stream().map(node -> node.data).toArray(size -> new Row[size]));

        for (Node<Row> node : tree.roots) {
            Object itemId = grid.addRow(node.data.first, String.valueOf(node.data.second));
            idToRow.put(itemId, node.data);
        }

        layout.addComponents(grid);
        layout.setMargin(true);
        layout.setSpacing(true);
        layout.setHeight("100%");

        setContent(layout);
    }

    Grid createGrid(Tree<Row> rowTree, Row... roots) {
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
            Object itemId = grid.addRow(row.first, String.valueOf(row.second));
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

        rowTree.getChildren(row).forEach(r -> {
            Object itemId = grid.addRow(r.first, String.valueOf(r.second));
            idToRow.put(itemId, r);
            grid.setDetailsVisible(itemId, true);
        });
        return grid;
    }


    private Tree<Row> createTree() {
        Row rot = new Row("Root", 1);
        Row root = new Row("Root", 1);
        Row rooot = new Row("Root", 1);

        HashSet<Node<Row>> roots = new HashSet<>();
        roots.add(new Node<>(rot));
        roots.add(new Node<>(root));
        roots.add(new Node<>(rooot));

        Tree<Row> rowTree = new Tree<>(roots);

        Row row = new Row("QQ", 2);
        rowTree.addChild(rot, row);
        rowTree.addChild(rot, new Row("QW", 2));

        rowTree.addChild(row, new Row("QWEASD", 3));
        rowTree.addChild(row, new Row("ASDZXC", 3));
        rowTree.addChild(row, new Row("ZXCASD", 3));

        Row qwe = new Row("ZXC", 2);
        Row asd = new Row("CXZ", 2);
        rowTree.addChild(root, qwe);
        rowTree.addChild(root, asd);

        rowTree.addChild(qwe, new Row("ZZZ", 3));
        rowTree.addChild(asd, new Row("CCC", 3));
        return rowTree;
    }

    @WebServlet(urlPatterns = "/*", name = "MyUIServlet", asyncSupported = true)
    @VaadinServletConfiguration(ui = MyUI.class, productionMode = false)
    public static class MyUIServlet extends VaadinServlet {
    }
}
