package nez.schema;

class XML { // built-in type

}

class Catalog {
	@Schematic
	public Book[] list;
}

class Book {
	@Schematic
	public String id;
	@Schematic
	public String author;
	@Schematic
	public String title;
	@Schematic
	public XML desc;
}

/**
 * <Book id="hoge"> <author>Kimio Kuramitsu</author> <title>Konoha</title>
 * <price>100</price> <desc> This is a really <b> grate </b> book in the
 * world.</desc> </Book>
 **/
