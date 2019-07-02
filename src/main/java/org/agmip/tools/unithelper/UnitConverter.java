package org.agmip.tools.unithelper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import static org.agmip.tools.unithelper.UnitConverter.UNIT_TYPE.values;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import ucar.units.BaseUnit;
import ucar.units.ConversionException;
import ucar.units.NameException;
import ucar.units.NoSuchUnitException;
import ucar.units.PrefixDBException;
import ucar.units.SpecificationException;
import ucar.units.Unit;
import ucar.units.UnitDB;
import ucar.units.UnitDBException;
import ucar.units.UnitDBManager;
import ucar.units.UnitFormat;
import ucar.units.UnitFormatManager;
import ucar.units.UnitParseException;
import ucar.units.UnitSystemException;
import ucar.units.UnknownUnit;

/**
 * Utility class which contains a collection of static method used for unit
 * conversion.
 *
 * @author Meng Zhang
 */
public class UnitConverter {
    
    public static enum UNIT_TYPE {
        ELECTRIC_CURRENT ("I"),
        LIMINOUS_INTENSITY ("J"),
        TEMPERATURE ("T"),
        MASS ("M"),
        LENGTH ("L"),
        AMOUNT_OF_SUBSTANCE ("N"),
        TIME ("t"),
        RADIAN ("Plane Angle"),
        STERADIAN ("Solid Angle"),
        UNKNOWN ("X");
        
        private final String code;
        UNIT_TYPE(String code) {
            this.code = code;
        }
        
        public String getCode() {
            return this.code;
        }
        
        public static UNIT_TYPE codeOf(String code) {
            if (code == null) {
                return UNKNOWN;
            }
            for (UNIT_TYPE type : values()) {
                if (type.getCode().equals(code)) {
                    return type;
                }
            }
            return UNKNOWN;
        }
    }

    private static final HashMap<String, String> AGMIP_UNIT = new HashMap();
    private static UnitDB DB = initDB();
    private static final UnitFormat PARSER = init();
    private static final HashMap<String, String> BASE_UNIT_MAP = initBaseUnitMap();

    private UnitConverter() {
    }
    
    private static UnitDB initDB() {
        try {
            return UnitDBManager.instance();
        } catch (UnitDBException ex) {
            System.err.println(ex.getMessage());
            return null;
        }
    }
    
    private static UnitFormat init() {
        AGMIP_UNIT.put("number", "count");
        AGMIP_UNIT.put("dap", "day");
        AGMIP_UNIT.put("doy", "day");
        AGMIP_UNIT.put("decimal_degree", "degree");
        AGMIP_UNIT.put("fraction", "1");
        AGMIP_UNIT.put("unitless", "1");
        AGMIP_UNIT.put("ratio", "1");
        try {
            for (String key : AGMIP_UNIT.keySet()) {
                DB.addAlias(key, AGMIP_UNIT.get(key));
            }
        } catch (UnitDBException | NoSuchUnitException | NameException ex) {
            System.err.println(ex.getMessage());
        }
        
        return UnitFormatManager.instance();
    }
    
    public static HashMap<String, String> initBaseUnitMap() {
        HashMap<String, String> ret = new HashMap();
        for (UNIT_TYPE type : UNIT_TYPE.values()) {
            ret.put(type.getCode(), type.toString());
        }
        return ret;
    }
    
    public static HashMap<String, String> getBaseUnitMap() {
        return BASE_UNIT_MAP;
    }

    public static BigDecimal convert(String fromUnit, String toUnit, String val) throws UnitParseException, SpecificationException, NoSuchUnitException, UnitDBException, PrefixDBException, UnitSystemException, ConversionException {
        return convert(fromUnit, toUnit, new BigDecimal(val));
    }

    public static BigDecimal convert(String fromUnit, String toUnit, String val, int scale) throws UnitParseException, SpecificationException, NoSuchUnitException, UnitDBException, PrefixDBException, UnitSystemException, ConversionException {
        return convert(fromUnit, toUnit, new BigDecimal(val), scale);
    }

    public static BigDecimal convert(String fromUnit, String toUnit, BigDecimal val) throws UnitParseException, SpecificationException, NoSuchUnitException, UnitDBException, PrefixDBException, UnitSystemException, ConversionException {
        Unit from = PARSER.parse(removeComment(fromUnit));
        Unit to = PARSER.parse(removeComment(toUnit));
        BigDecimal ret = new BigDecimal(from.convertTo(val.doubleValue(), to));
        int scale = ret.scale() + val.precision() - ret.precision();
        ret = ret.setScale(scale + 1, RoundingMode.HALF_UP);
        BigDecimal alt = ret.setScale(scale, RoundingMode.HALF_UP);
        while (ret.doubleValue() == alt.doubleValue()) {
            ret = alt;
            if (scale > 0) {
                scale--;
                alt = alt.setScale(scale, RoundingMode.HALF_UP);
            } else {
                break;
            }
        }
        return ret;
    }

    public static BigDecimal convert(String fromUnit, String toUnit, BigDecimal val, int scale) throws UnitParseException, SpecificationException, NoSuchUnitException, UnitDBException, PrefixDBException, UnitSystemException, ConversionException {
        Unit from = PARSER.parse(removeComment(fromUnit));
        Unit to = PARSER.parse(removeComment(toUnit));
        return new BigDecimal(from.convertTo(val.doubleValue(), to)).setScale(scale, RoundingMode.HALF_UP);
    }

    public static JSONObject convertToJsonObj(String fromUnit, String toUnit, String val) {
        return convertToJsonObj(fromUnit, toUnit, new BigDecimal(val));
    }

    public static JSONObject convertToJsonObj(String fromUnit, String toUnit, String val, int scale) {
        return convertToJsonObj(fromUnit, toUnit, new BigDecimal(val), scale);
    }

    public static JSONObject convertToJsonObj(String fromUnit, String toUnit, BigDecimal val) {
//        return convertToJsonObj(fromUnit, toUnit, val, val.scale());
        JSONObject ret = new JSONObject();
        ret.put("unit_from", fromUnit);
        ret.put("unit_to", toUnit);
        ret.put("value_from", val.toPlainString());
        try {
            ret.put("value_to", convert(fromUnit, toUnit, val).toPlainString());
            ret.put("status", "0");
            ret.put("message", "successful");
        } catch (SpecificationException | UnitDBException | PrefixDBException | UnitSystemException | ConversionException ex) {
            ret.put("status", "1");
            ret.put("message", ex.getMessage());
        } catch (Exception ex) {
//            Logger.getLogger(UnitConverter.class.getName()).log(Level.SEVERE, null, ex);
            ret.put("status", "1");
            ret.put("message", "undefined unit");
        }
//        catch (SpecificationException ex) {
//            Logger.getLogger(UnitConverter.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (UnitDBException ex) {
//            Logger.getLogger(UnitConverter.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (PrefixDBException ex) {
//            Logger.getLogger(UnitConverter.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (UnitSystemException ex) {
//            Logger.getLogger(UnitConverter.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (ConversionException ex) {
//            Logger.getLogger(UnitConverter.class.getName()).log(Level.SEVERE, null, ex);
//        }

        return ret;
    }

    public static JSONObject convertToJsonObj(String fromUnit, String toUnit, BigDecimal val, int scale) {
        JSONObject ret = new JSONObject();
        ret.put("unit_from", fromUnit);
        ret.put("unit_to", toUnit);
        ret.put("value_from", val.toPlainString());
        try {
            ret.put("value_to", convert(fromUnit, toUnit, val, scale).toPlainString());
            ret.put("status", "0");
            ret.put("message", "successful");
        } catch (SpecificationException | UnitDBException | PrefixDBException | UnitSystemException | ConversionException ex) {
            ret.put("status", "1");
            ret.put("message", ex.getMessage());
        } catch (Exception ex) {
//            Logger.getLogger(UnitConverter.class.getName()).log(Level.SEVERE, null, ex);
            ret.put("status", "1");
            ret.put("message", "undefined unit");
        }
//        catch (SpecificationException ex) {
//            Logger.getLogger(UnitConverter.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (UnitDBException ex) {
//            Logger.getLogger(UnitConverter.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (PrefixDBException ex) {
//            Logger.getLogger(UnitConverter.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (UnitSystemException ex) {
//            Logger.getLogger(UnitConverter.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (ConversionException ex) {
//            Logger.getLogger(UnitConverter.class.getName()).log(Level.SEVERE, null, ex);
//        }

        return ret;
    }

    public static String convertToJsonStr(String fromUnit, String toUnit, String val) {
        return convertToJsonStr(fromUnit, toUnit, new BigDecimal(val));
    }

    public static String convertToJsonStr(String fromUnit, String toUnit, String val, int scale) {
        return convertToJsonStr(fromUnit, toUnit, new BigDecimal(val), scale);
    }

    public static String convertToJsonStr(String fromUnit, String toUnit, BigDecimal val) {
        return convertToJsonObj(fromUnit, toUnit, val).toJSONString();
    }

    public static String convertToJsonStr(String fromUnit, String toUnit, BigDecimal val, int scale) {
        return convertToJsonObj(fromUnit, toUnit, val, scale).toJSONString();
    }

    protected static String removeComment(String unit) {
        return unit.replaceAll("[\\./]?\\[\\S*\\]\\^?-?\\d*", "").replaceAll("\\s", "");
    }

    public static boolean isValid(String unitStr) {
        try {
            return PARSER.parse(removeComment(unitStr)) != null;
        } catch (Exception ex) {
            return false;
        }
    }

    public static String getDescp(String unitStr) {
        String unitStrNoComment = removeComment(unitStr);
        if (AGMIP_UNIT.containsKey(unitStrNoComment)) {
            String agmipRet = AGMIP_UNIT.get(unitStrNoComment);
            if (agmipRet.equals("1")) {
                return unitStrNoComment;
            }
        }
        try {
            Unit unit = PARSER.parse(unitStrNoComment);
            String ret = unit.toString();
            if (unit instanceof UnknownUnit) {
                ret = "";
            } else if (unit instanceof BaseUnit) {
                ret = unit.getName();
            } else if (unit.getDerivedUnit() instanceof UnknownUnit) {
                ret = "";
            }
            return ret;
        } catch (Exception ex) {
            return "";
        }
    }
    
    public static ArrayList<Unit> listUnit(String unitTypeCode) {
        return listUnit(UNIT_TYPE.codeOf(unitTypeCode));
    }
    
    public static ArrayList<Unit> listUnit(UNIT_TYPE type) {
        
        Iterator it = DB.getIterator();
        ArrayList<Unit> ret = new ArrayList();
        while(it.hasNext()) {
            Unit unit = (Unit) it.next();
            if (unit.getDerivedUnit().getQuantityDimension().toString().equals(type.getCode())) {
                ret.add(unit);
            }
        }
        return ret;
    }
    
    public static JSONArray listUnitJsonArray(String unitTypeCode) {
        return listUnitJsonArray(UNIT_TYPE.codeOf(unitTypeCode));
    }
    
    public static JSONArray listUnitJsonArray(UNIT_TYPE type) {
        JSONArray ret = new JSONArray();
        for (Unit unit : listUnit(type)) {
            JSONObject data = new JSONObject();
            data.put("name", unit.getName());
            data.put("type", type.toString());
            data.put("type_code", type.getCode());
            data.put("expression", unit.getCanonicalString());
            if (unit.getSymbol() == null) {
                data.put("symbol", unit.getName().replaceAll("\\s", "_"));
            } else {
                data.put("symbol", unit.getSymbol());
            }
            ret.add(data);
        }
        
        return ret;
    }
    
    public static String listUnitJsonStr(String unitTypeCode) {
        return listUnitJsonStr(UNIT_TYPE.codeOf(unitTypeCode));
    }
    
    public static String listUnitJsonStr(UNIT_TYPE type) {
        return listUnitJsonArray(type).toJSONString();
    }
}
