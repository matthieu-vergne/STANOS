package fr.vergne.stanos.gui.scene.graph.cell;

import javafx.scene.control.Label;

public class LabelCell extends Cell {

    public LabelCell(String id) {
        super(id);

        Label view = new Label(id);

        setView(view);

    }

}

