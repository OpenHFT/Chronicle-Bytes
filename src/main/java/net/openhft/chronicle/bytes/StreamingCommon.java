/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.bytes;

import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

interface StreamingCommon<S extends StreamingCommon<S>> extends RandomCommon {

    /**
     * Set the readPosition= writePosition = start, writeLimit = capacity
     *
     * @return this
     */
    @NotNull
    S clear();


    /**
     * Skip a number of bytes by moving the readPosition. Must be less than or equal to the readLimit.
     * @param bytesToSkip bytes to skip.
     * @return this
     */
    S readSkip(long bytesToSkip) throws BufferUnderflowException, IORuntimeException;

    /**
     * Skip a number of bytes by moving the readPosition. Must be less than or equal to the readLimit.
     * @param bytesToSkip bytes to skip.
     * @return this
     */
    S writeSkip(long bytesToSkip) throws BufferOverflowException, IORuntimeException;

}
