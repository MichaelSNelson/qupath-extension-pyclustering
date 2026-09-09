"""
QP-CAT Windows environment checker.

Finds DLL/PYD files in the QP-CAT Python environment that Windows cannot load:
wrong architecture, truncated, or otherwise not a valid PE binary. This is what
produces:

    ImportError: DLL load failed while importing _igraph:
    %1 is not a valid Win32 application.

Windows reports that error against the module you imported, NOT against the file
that is actually broken -- the bad file is usually something the module depends
on. This scans every binary in the environment so the culprit is named.

Run it with the environment's own Python (adjust the path if you moved the env):

    "%USERPROFILE%\\.local\\share\\appose\\qupath-qpcat\\.pixi\\envs\\default\\python.exe" qpcat_check_dlls.py
"""

import os
import struct
import sys

MACHINE = {
    0x8664: "x64",
    0x014C: "x86 (32-bit)",
    0xAA64: "ARM64",
    0x01C4: "ARM (32-bit)",
}
EXPECTED = 0x8664


def pe_machine(path):
    """(machine_code, note). machine_code is None when the file is not a valid PE."""
    try:
        size = os.path.getsize(path)
        if size == 0:
            return None, "EMPTY FILE (0 bytes)"
        with open(path, "rb") as fh:
            if fh.read(2) != b"MZ":
                return None, "no MZ header -- not a Windows binary"
            fh.seek(0x3C)
            raw = fh.read(4)
            if len(raw) < 4:
                return None, "truncated before PE offset (size %d)" % size
            off = struct.unpack("<I", raw)[0]
            if off <= 0 or off + 6 > size:
                return None, "PE offset %d outside file (size %d) -- TRUNCATED" % (off, size)
            fh.seek(off)
            if fh.read(4) != b"PE\0\0":
                return None, "bad PE signature at offset %d" % off
            return struct.unpack("<H", fh.read(2))[0], ""
    except Exception as exc:                      # unreadable, locked, etc.
        return None, "could not read: %s" % exc


def main():
    root = sys.argv[1] if len(sys.argv) > 1 else os.path.dirname(sys.executable)
    print("Python   : %s" % sys.executable)
    print("Bits     : %d-bit" % (64 if sys.maxsize > 2**32 else 32))
    print("Scanning : %s\n" % root)

    scanned = bad = 0
    for dirpath, _dirs, files in os.walk(root):
        for name in files:
            if not name.lower().endswith((".dll", ".pyd")):
                continue
            path = os.path.join(dirpath, name)
            scanned += 1
            machine, note = pe_machine(path)
            if machine is None:
                bad += 1
                print("BROKEN   %s\n           %s" % (path, note))
            elif machine != EXPECTED:
                bad += 1
                print("WRONG ARCH  %s\n           is %s, expected x64"
                      % (path, MACHINE.get(machine, "unknown 0x%04X" % machine)))

    print("\n%d binaries scanned, %d problem(s) found." % (scanned, bad))
    if bad == 0:
        print(
            "\nEvery binary is a valid 64-bit PE, so the failure is not a corrupt\n"
            "file in the environment. Report this output on the forum thread."
        )
    else:
        print(
            "\nThose files are why the import fails. They are almost always a bad\n"
            "download or antivirus interference rather than a packaging fault.\n"
            "Fix: delete the environment AND the package cache, then let QP-CAT\n"
            "rebuild -- rebuilding alone re-extracts the same corrupt bytes."
        )


if __name__ == "__main__":
    main()
