package com.google.errorprone.bugpatterns;

public class ReturnPostIncDecCheckerPositiveCases {
	
	// BUG: Diagnostic contains: Do not return int post increment or decrement.
	public int returnsPostInc(int x) {
		return x++;
	}

	// BUG: Diagnostic contains: Do not return int post increment or decrement.
	public int returnsPostDec(int x) {
		return x--;
	}

	// BUG: Diagnostic contains: Do not return int post increment or decrement.
	public int sometimesReturnsPostIncDec(int z) {
		if (z == 10) {
			return z;
		} else if (z > 10) {
			return z--;
		} else {
			return z++;
		}
	}

	// BUG: Diagnostic contains: Do not return int post increment or decrement.
	public int returnsCastedPostInc() {
		double a = 1.0;
		return (int) ((double) a++);
	}

	// BUG: Diagnostic contains: Do not return int post increment or decrement.
	public int returnsComplexOperationsWithPostIncDec(int x) {
		int y = 5;
		int z = x - y;
		return ((x-- + y) + z++);
	}
	
}	