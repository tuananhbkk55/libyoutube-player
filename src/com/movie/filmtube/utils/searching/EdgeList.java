package com.movie.filmtube.utils.searching;

/**
   Simple interface for mapping bytes to States.
 */
interface EdgeList {
    State get(byte ch);
    void put(byte ch, State state);
    byte[] keys();
}
