# Bitmap Operation on Redis  

* An goods stock model store on redis bitmap  
* Command of redis using are SETBIT, BITCOUNT, BITPOS, GETBIT, SET, DEL 

## Store in redis like:
	
1.overall

     ---------------------------------------
     | 1 | 1 | 1 | 1 | 1 | 0 | 0 | ... | 0 | ... back
     ---------------------------------------
	 
	1 - has one stock in this unit
	0 - empty in this unit

2.deduct

     ------------------------------------------
     | 0 | 0 | 1~0 | 1 | 1 | 0 | 0 | ... | 0 | ... back
     ------------------------------------------	
       |        |
   deducted  deducting one

3.send back

           ------------------------------------------
     front | 0 | 1 | 0 | 1 | 1 | 0 | 0 | ... | 0 | ... back
           ------------------------------------------
                 |   
           send back one 

4.refill 

    <<before refilil>>
           ------------------------------------------
     front | 0 | 1 | 0 | 1 | 1 | 0 | 0 | ... | 0 | ... back
           ------------------------------------------

    <<after refill>>
           ------------------------------------------
     front | 0 | 0 | 0 | 1 | 1 | 1 | 1 | ... | 1 | ... back
           ------------------------------------------
                		 |    
           		refill from here 

## Using
	* fetch 1 will return the position also
	* sendback 1 must provice the position be fetched   

## License

Copyright (c) 2014-2015, Peiyuan Feng <fengpeiyuan@gmail.com>.

This module is licensed under the terms of the BSD license.
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

* Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
* Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
