package com.mycucumbertest.pages;

import com.microsoft.playwright.Page;

public abstract class BasePage {

    protected final Page page;

    protected BasePage(Page page) {
        this.page = page;
    }

    public String title() {
        return page.title();
    }

    public String url() {
        return page.url();
    }
}