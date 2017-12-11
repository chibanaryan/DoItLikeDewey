package com.ryanchibana.doitlikedewey;

import java.util.List;
import java.util.Stack;

/**
 * Created by colonelchibbers on 12/10/2017.
 * This represents the search result of executing a query.
 */

public class SearchResult {
    public int hierarchyLevel;
    public String result;
    public List<String> categoryList;
    public Stack<String> hierarchyChain;

    public SearchResult(int hierarchyLevel, String result, List<String> categoryList, Stack<String> hierarchyChain) {
        this.hierarchyLevel = hierarchyLevel;
        this.result = result;
        this.categoryList = categoryList;
        this.hierarchyChain = hierarchyChain;
    }
}
