package ru.orangesoftware.financisto.test;

import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.model.Attribute;
import ru.orangesoftware.financisto.model.Category;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 4/28/11 11:29 PM
 */
public class CategoryBuilder {

    private final DatabaseAdapter db;
    private final Category category = new Category();

    /**
     * A
     * - A1
     * -- AA1
     * - A2
     * B
     */
    public static Map<String, Category> createDefaultHierarchy(DatabaseAdapter db) {
        Category a = new CategoryBuilder(db).withTitle("A").create();
        Category a1 = new CategoryBuilder(db).withParent(a).withTitle("A1").create();
        new CategoryBuilder(db).withParent(a1).withTitle("AA1").create();
        new CategoryBuilder(db).withParent(a).withTitle("A2").create();
        new CategoryBuilder(db).withTitle("B").income().create();
        return allCategoriesAsMap(db);
    }

    private static Map<String, Category> allCategoriesAsMap(DatabaseAdapter db) {
        HashMap<String, Category> map = new HashMap<String, Category>();
        List<Category> categories = db.getAllCategoriesList();
        for (Category category : categories) {
            map.put(category.title, category);
        }
        return map;
    }

    public static Category split(DatabaseAdapter db) {
        return db.getCategory(Category.SPLIT_CATEGORY_ID);
    }

    public static Category noCategory(DatabaseAdapter db) {
        return db.getCategory(Category.NO_CATEGORY_ID);
    }

    private CategoryBuilder(DatabaseAdapter db) {
        this.db = db;
    }

    public CategoryBuilder withTitle(String title) {
        category.title = title;
        return this;
    }

    public CategoryBuilder withParent(Category parent) {
        category.parent = parent;
        return this;
    }

    private CategoryBuilder income() {
        category.makeThisCategoryIncome();
        return this;
    }

    public Category create() {
        category.id = db.insertOrUpdate(category, Collections.<Attribute>emptyList());
        return category;
    }

}
