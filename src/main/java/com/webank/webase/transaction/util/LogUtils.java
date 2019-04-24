package com.webank.webase.transaction.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LogUtils
 */
public class LogUtils {

    private static final Logger MONITORBUSINESS  = LoggerFactory.getLogger("monitorBusiness");
    
    private static final Logger MONITORABNORMAL  = LoggerFactory.getLogger("monitorAbnormal");

    public static Logger monitorBusinessLogger() {
        return MONITORBUSINESS;
    }
    
    public static Logger monitorAbnormalLogger() {
    	return MONITORABNORMAL;
    }

}
