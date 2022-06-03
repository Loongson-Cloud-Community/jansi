/*
 * Copyright (C) 2022 the original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.jansi;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

import static org.fusesource.jansi.internal.CLibrary.TIOCGWINSZ;
import static org.fusesource.jansi.internal.CLibrary.WinSize;
import static org.fusesource.jansi.internal.CLibrary.ioctl;
import static org.fusesource.jansi.internal.CLibrary.isatty;
import static org.fusesource.jansi.internal.Kernel32.CONSOLE_SCREEN_BUFFER_INFO;
import static org.fusesource.jansi.internal.Kernel32.GetConsoleMode;
import static org.fusesource.jansi.internal.Kernel32.GetConsoleScreenBufferInfo;
import static org.fusesource.jansi.internal.Kernel32.GetStdHandle;
import static org.fusesource.jansi.internal.Kernel32.STD_ERROR_HANDLE;
import static org.fusesource.jansi.internal.Kernel32.STD_OUTPUT_HANDLE;
import static org.fusesource.jansi.internal.Kernel32.SetConsoleMode;

public class AnsiConsoleHelper {

    static class CLibrary  {

        static short getTerminalWidth(int fd) {
            WinSize sz = new WinSize();
            ioctl( fd, TIOCGWINSZ, sz);
            return sz.ws_col;
        }

        static int isTty(int fd) {
            return isatty(fd);
        }
    }

    static class CLibraryHelperJep424
    {
        static GroupLayout wsLayout;
        static MethodHandle ioctl;
        static VarHandle ws_col;
        static MethodHandle isatty;
        static {
            wsLayout = MemoryLayout.structLayout(
                    ValueLayout.JAVA_SHORT.withName("ws_row"),
                    ValueLayout.JAVA_SHORT.withName("ws_col"),
                    ValueLayout.JAVA_SHORT,
                    ValueLayout.JAVA_SHORT
            );
            ws_col = wsLayout.varHandle( MemoryLayout.PathElement.groupElement("ws_col"));
            Linker linker = Linker.nativeLinker();
            ioctl = linker.downcallHandle(
                    linker.defaultLookup().lookup("ioctl").get(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
            isatty = linker.downcallHandle(
                    linker.defaultLookup().lookup("isatty").get(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        }

        static short getTerminalWidth(int fd) {
            MemorySegment segment = MemorySegment.allocateNative(wsLayout, MemorySession.openImplicit());
            try {
                int res = (int) ioctl.invoke(fd, TIOCGWINSZ, segment.address());
                return (short) ws_col.get( segment );
            } catch (Throwable e) {
                throw new RuntimeException("Unable to ioctl(TIOCGWINSZ)", e);
            }
        }

        static int isTty(int fd) {
            try {
                return (int) isatty.invoke( fd );
            } catch (Throwable e) {
                throw new RuntimeException("Unable to call isatty", e);
            }
        }
    }

    static class Kernel32 {

        static int getTerminalWidth(long console) {
            CONSOLE_SCREEN_BUFFER_INFO info = new CONSOLE_SCREEN_BUFFER_INFO();
            GetConsoleScreenBufferInfo(console, info);
            return info.windowWidth();
        }

        static long getStdHandle(boolean stdout) {
            return GetStdHandle(stdout ? STD_OUTPUT_HANDLE : STD_ERROR_HANDLE);
        }

        static int getConsoleMode(long console, int[] mode) {
            return GetConsoleMode(console, mode);
        }

        static int setConsoleMode(long console, int mode) {
            return SetConsoleMode(console, mode);
        }
    }

    static class Kernel32Jep424 {


    }

}
