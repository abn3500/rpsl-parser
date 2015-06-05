/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package comp3500.abn.emitters.odlconfig;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class ODLReconnectStrategyTest {

	@Test
	public void testFieldAssignment() {
		Map<String, String> argMap = new HashMap<String, String>();
		argMap.put("BGP_RECONNECT_SLEEP_MIN", "1001");
		argMap.put("BGP_RECONNECT_SLEEP_MAX", "180001");
		argMap.put("BGP_RECONNCET_CONNECT_TIME", "5001");
		argMap.put("BGP_RECONNCET_SLEEP_FACTOR", "2.1");
		
		ODLReconnectStrategy clean = new ODLReconnectStrategy(),
							 modified = new ODLReconnectStrategy(argMap);
		
		assertNotEquals(clean.BGP_RECONNECT_SLEEP_MIN, modified.BGP_RECONNECT_SLEEP_MIN);
		assertNotEquals(clean.BGP_RECONNECT_SLEEP_MAX, modified.BGP_RECONNECT_SLEEP_MAX);
		assertNotEquals(clean.BGP_RECONNCET_CONNECT_TIME, modified.BGP_RECONNCET_CONNECT_TIME);
		assertNotEquals(clean.BGP_RECONNCET_SLEEP_FACTOR, modified.BGP_RECONNCET_SLEEP_FACTOR);
	}

}