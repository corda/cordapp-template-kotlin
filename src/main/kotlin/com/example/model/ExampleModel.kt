package com.example.model

/**
 * A simple class with arbitrary data to be written to the ledger. In reality this could be a representation
 * of some kind of trade such as an IRS swap for example.
 */
data class ExampleModel(val swapRef: String, val data: String)
