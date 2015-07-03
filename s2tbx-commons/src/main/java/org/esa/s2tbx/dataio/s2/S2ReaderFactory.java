/*
 *
 * Copyright (C) 2014-2015 CS SI
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 *
 */
package org.esa.s2tbx.dataio.s2;

/**
 * @author Nicolas Ducoin
 */
public abstract class S2ReaderFactory {


    private static S2Config configInstance = null;

    protected abstract S2Config createS2Config();

    public S2Config getConfigInstance() {
        if(configInstance == null) {
            configInstance = createS2Config();
        }
        return configInstance;
    }
}