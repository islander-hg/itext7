/*
    This file is part of the iText (R) project.
    Copyright (c) 1998-2020 iText Group NV
    Authors: iText Software.

    This program is offered under a commercial and under the AGPL license.
    For commercial licensing, contact us at https://itextpdf.com/sales.  For AGPL licensing, see below.

    AGPL licensing:
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.itextpdf.styledxmlparser.css.util;

import com.itextpdf.kernel.colors.WebColors;
import com.itextpdf.styledxmlparser.css.CommonCssConstants;

/**
 * Utilities class for CSS types validating operations.
 */
public final class CssTypesValidationUtils {
    // TODO (DEVSIX-3595) The list of the angle measurements is not full. Required to
    //  add 'turn' units to array and move this array to the CommonCssConstants
    private static final String[] ANGLE_MEASUREMENTS_VALUES = new String[] {CommonCssConstants.DEG, CommonCssConstants.GRAD,
            CommonCssConstants.RAD};

    // TODO (DEVSIX-3596) The list of the relative measurements is not full.
    //  Add new relative units to array and move this array to the CommonCssConstants
    private static final String[] RELATIVE_MEASUREMENTS_VALUES = new String[] {CommonCssConstants.PERCENTAGE,
            CommonCssConstants.EM, CommonCssConstants.EX, CommonCssConstants.REM};

    /**
     * Checks whether a string contains an allowed metric unit in HTML/CSS; rad, deg and grad.
     *
     * @param value the string that needs to be checked
     * @return boolean true if value contains an allowed angle value
     */
    public static boolean isAngleValue(final String value) {
        if (value == null) {
            return false;
        }
        for (String metricPostfix : ANGLE_MEASUREMENTS_VALUES) {
            if (value.endsWith(metricPostfix) && isNumericValue(
                    value.substring(0, value.length() - metricPostfix.length()).trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a data is base 64 encoded.
     *
     * @param data the data
     * @return true, if the data is base 64 encoded
     */
    public static boolean isBase64Data(String data) {
        return data.matches("^data:([^\\s]*);base64,([^\\s]*)");
    }

    /**
     * Checks if a value is a color property.
     *
     * @param value the value
     * @return true, if the value contains a color property
     */
    public static boolean isColorProperty(String value) {
        return value.startsWith("rgb(") || value.startsWith("rgba(") || value.startsWith("#")
                || WebColors.NAMES.containsKey(value.toLowerCase()) || CommonCssConstants.TRANSPARENT.equals(value);
    }

    /**
     * Checks whether a string contains an allowed value relative to parent value.
     *
     * @param value the string that needs to be checked
     * @return boolean true if value contains a em value
     */
    public static boolean isEmValue(final String value) {
        return value != null && value.endsWith(CommonCssConstants.EM) && isNumericValue(
                value.substring(0, value.length() - CommonCssConstants.EM.length()).trim());
    }

    /**
     * Checks whether a string contains an allowed value relative to element font height.
     *
     * @param value the string that needs to be checked
     * @return boolean true if value contains a ex value
     */
    public static boolean isExValue(final String value) {
        return value != null && value.endsWith(CommonCssConstants.EX) && isNumericValue(
                value.substring(0, value.length() - CommonCssConstants.EX.length()).trim());
    }

    /**
     * Checks whether a string contains an allowed metric unit in HTML/CSS; px, in, cm, mm, pc, Q or pt.
     *
     * @param value the string that needs to be checked
     * @return boolean true if value contains an allowed metric value
     */
    public static boolean isMetricValue(final String value) {
        if (value == null) {
            return false;
        }
        for (String metricPostfix : CommonCssConstants.METRIC_MEASUREMENTS_VALUES) {
            if (value.endsWith(metricPostfix) && isNumericValue(
                    value.substring(0, value.length() - metricPostfix.length()).trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether a string matches a negative value (e.g. -123, -2em, -0.123).
     * All these metric values are allowed in HTML/CSS.
     *
     * @param value the string that needs to be checked
     * @return true if value is negative
     */
    public static boolean isNegativeValue(final String value) {
        if (value == null) {
            return false;
        }
        if (isNumericValue(value) || isRelativeValue(value)) {
            return value.startsWith("-");
        }
        return false;
    }

    /**
     * Checks whether a string matches a numeric value (e.g. 123, 1.23, .123). All these metric values are allowed in
     * HTML/CSS.
     *
     * @param value the string that needs to be checked
     * @return boolean true if value contains an allowed metric value
     */
    public static boolean isNumericValue(final String value) {
        return value != null && (value.matches("^[-+]?\\d\\d*\\.\\d*$")
                || value.matches("^[-+]?\\d\\d*$")
                || value.matches("^[-+]?\\.\\d\\d*$"));
    }

    /**
     * Checks whether a string contains a percentage value
     *
     * @param value the string that needs to be checked
     * @return boolean true if value contains an allowed percentage value
     */
    public static boolean isPercentageValue(final String value) {
        return value != null && value.endsWith(CommonCssConstants.PERCENTAGE) && isNumericValue(
                value.substring(0, value.length() - CommonCssConstants.PERCENTAGE.length()).trim());
    }

    /**
     * Checks whether a string contains an allowed value relative to previously set value.
     *
     * @param value the string that needs to be checked
     * @return boolean true if value contains an allowed metric value
     */
    public static boolean isRelativeValue(final String value) {
        if (value == null) {
            return false;
        }
        for (String relativePostfix : RELATIVE_MEASUREMENTS_VALUES) {
            if (value.endsWith(relativePostfix) && isNumericValue(
                    value.substring(0, value.length() - relativePostfix.length()).trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether a string contains an allowed value relative to previously set root value.
     *
     * @param value the string that needs to be checked
     * @return boolean true if value contains a rem value
     */
    public static boolean isRemValue(final String value) {
        return value != null && value.endsWith(CommonCssConstants.REM) && isNumericValue(
                value.substring(0, value.length() - CommonCssConstants.REM.length()).trim());
    }

    /**
     * Checks if a string is in a valid format.
     *
     * @param value the string that needs to be checked
     * @return boolean true if value is in a valid format
     */
    public static boolean isValidNumericValue(final String value) {
        if (value == null || value.contains(" ")) {
            return false;
        }
        return isRelativeValue(value) || isMetricValue(value) || isNumericValue(value);
    }

    /**
     * Checks if value is initial, inherit or unset.
     *
     * @param value value to check
     * @return true if value is initial, inherit or unset. false otherwise
     */
    public static boolean isInitialOrInheritOrUnset(String value) {
        return CommonCssConstants.INITIAL.equals(value) ||
                CommonCssConstants.INHERIT.equals(value) ||
                CommonCssConstants.UNSET.equals(value);
    }

    /**
     * Creates a new {@link CssTypesValidationUtils} instance.
     */
    private CssTypesValidationUtils() {
        // Empty constructor
    }
}
