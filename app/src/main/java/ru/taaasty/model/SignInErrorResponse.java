package ru.taaasty.model;

import java.util.Map;

/**
* Created by alexey on 09.07.14.
*/
public class SignInErrorResponse {

    int responseCode;

    String errorCode;

    String error;

    Map<String, String> fields;

}
