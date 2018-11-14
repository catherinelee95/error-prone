package com.google.errorprone.bugpatterns;

public class ReturnPostIncDecCheckerPositiveCases {
	
	  // BUG: Diagnostic contains: Do not return increment.
	  public int returnsNull(int z) {
		int x = 1;	
		int y = 2;
		if(z == 3) {
		    return z - y;
		} else {
			return x++;	
		}
	  }
}
