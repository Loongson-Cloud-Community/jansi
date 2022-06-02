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


}
