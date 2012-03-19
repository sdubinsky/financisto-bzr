/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.model;

import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 3/18/12 8:21 PM
 */
public class CategoryTreeNavigator {

    private final Stack<CategoryTree<Category>> categoriesStack = new Stack<CategoryTree<Category>>();

    public CategoryTree<Category> categories;
    public long selectedCategoryId = 0;

    public CategoryTreeNavigator(CategoryTree<Category> categories) {
        this.categories = categories;
    }

    public void selectCategory(long selectedCategoryId) {
        Map<Long, Category> map = categories.asMap();
        Category selectedCategory = map.get(selectedCategoryId);
        if (selectedCategory != null) {
            Stack<Long> path = new Stack<Long>();
            Category parent = selectedCategory.parent;
            while (parent != null) {
                path.push(parent.id);
                parent = parent.parent;
            }
            while (!path.isEmpty()) {
                navigateTo(path.pop());
            }
            this.selectedCategoryId = selectedCategoryId;
        }
    }

    public void tagCategories(Category parent) {
        if (categories.size() > 0 && categories.getAt(0).id != parent.id) {
            Category copy = new Category();
            copy.id = parent.id;
            copy.title = parent.title;
            if (parent.isIncome()) {
                copy.makeThisCategoryIncome();
            }
            categories.insertAtTop(copy);
        }
        StringBuilder sb = new StringBuilder();
        for (Category c : categories) {
            if (c.tag == null && c.hasChildren()) {
                sb.setLength(0);
                CategoryTree<Category> children = c.children;
                for (Category child : children) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(child.title);
                }
                c.tag = sb.toString();
            }
        }
    }

    public boolean goBack() {
        if (!categoriesStack.isEmpty()) {
            Category selectedCategory = findCategory(selectedCategoryId);
            selectedCategoryId = selectedCategory.getParentId();
            categories = categoriesStack.pop();
            return true;
        }
        return false;
    }

    public boolean canGoBack() {
        return !categoriesStack.isEmpty();
    }

    public boolean navigateTo(long categoryId) {
        Category selectedCategory = findCategory(categoryId);
        if (selectedCategory != null) {
            selectedCategoryId = selectedCategory.id;
            if (selectedCategory.hasChildren()) {
                categoriesStack.push(categories);
                categories = selectedCategory.children;
                tagCategories(selectedCategory);
                return true;
            }
        }
        return false;
    }

    private Category findCategory(long categoryId) {
        for (Category category : categories) {
            if (category.id == categoryId) {
                return category;
            }
        }
        return null;
    }

    public boolean isSelected(long categoryId) {
        return selectedCategoryId == categoryId;
    }
    
    public List<Category> getSelectedRoots() {
        return categories.getRoots();
    }

}
