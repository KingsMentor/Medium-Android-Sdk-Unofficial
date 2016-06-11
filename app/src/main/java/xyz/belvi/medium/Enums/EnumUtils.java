package xyz.belvi.medium.Enums;

/**
 * Created by zone2 on 6/11/16.
 */
public class EnumUtils {

    public static ErrorCodes getErrorObjByCode(int code) {
        for (ErrorCodes errorCode : ErrorCodes.values()) {
            if (errorCode.getErrorCode() == code)
                return errorCode;
        }
        return ErrorCodes.UNKNOWN;
    }

    public static Scope getScopeByName(String scopeName) {
        for (Scope scope : Scope.values()) {
            if (scope.getScopeName().equalsIgnoreCase(scopeName))
                return scope;
        }
        return null;
    }

}
