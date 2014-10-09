package com.jobscience.search.dao;

import javax.inject.Singleton;

import com.google.inject.Inject;

@Singleton
public class DaoRwHelper extends DaoHelper {

	@Inject
    public void init() {
		setDB(false);
    }
}
