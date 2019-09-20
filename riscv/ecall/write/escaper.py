
with open("final_output.bin", "rb") as f:
    bytes = f.read()
    s = ""
    for byte in bytes:
        h = hex(byte)[2:]
        if len(h) == 1:
            s += "\\u000" + h
        else:
            s += "\\u00" + h

    print(s)