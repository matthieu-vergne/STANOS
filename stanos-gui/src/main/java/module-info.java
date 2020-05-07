module fr.vergne.stanos.gui {
	requires javafx.controls;
	requires javafx.graphics;
	requires fr.vergne.stanos;
	
	opens fr.vergne.stanos.gui to javafx.graphics;
}