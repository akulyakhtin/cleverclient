package io.github.sashirestela.cleverclient.util;

import java.math.BigInteger;
import java.security.SecureRandom;

public class Constant {

    private Constant() {
    }
    public static final String BOUNDARY_VALUE = new BigInteger(256, new SecureRandom()).toString();

    public static final String REGEX_PATH_PARAM_URL = "\\{(.*?)\\}";

}