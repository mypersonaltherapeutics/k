// Copyright (c) 2016 K Team. All Rights Reserved.

module B-SYNTAX
    syntax Pgm
endmodule

module B
    imports B-SYNTAX

configuration <T> <k> $PGM:Pgm </k> </T>

syntax KList ::= B
syntax Bag ::= B
syntax B

endmodule
