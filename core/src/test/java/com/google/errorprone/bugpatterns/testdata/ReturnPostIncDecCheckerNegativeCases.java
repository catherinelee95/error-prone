package com.google.errorprone.bugpatterns;

public class ReturnPostIncDecCheckerNegativeCases {
	
	public int returnsPreInc(int a) {
		return ++a;
	}

	public int returnsPreDec(int a) {
		return --a;
	}
	
	public String returnPostIncString() {
		return "x++";
	}

	public int sometimesReturnsPreIncDec(int a) {
		int b = 0;
		int c = 5;
		if (a == 10) {
			return a;
		} else if (a > 10) {
			return --b;
		} else {
			return ++c;
		}
	}
}