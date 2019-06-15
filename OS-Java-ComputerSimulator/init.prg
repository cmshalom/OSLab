stack_size: equ   100
data_segments: equ 10
// Addresses
me:         equ   96
myParent:   equ   100
shmid:      equ   104
shmpag:     equ   108

// Other constants
children:   equ   3
shmkey:     equ   12345
childDelay: equ  10

main:       CALL  storeProcessIDs
            PUSH  [me]
            PUSH  [myParent]
            MOV   DI, 0
            MOV   [DI],123   // Mark the Exstra Segment with 123 
            shmget shmkey
            MOV   shmid, AX
            log   "SHMGET RETURNS"
            log   AX
            
            MOV   CX, 0
forkLoop:   CMP   CX, children
            JZ    wait
			fork
            CMP   AX, 0
            JNZ   parent
            CALL  child
parent:     INC   CX
            JMP   forkLoop
wait:       LOG   "PARENT WAITS"
 			CALL  waitAndReportChildren
finalLoop:  JMP   finalLoop

//
//
//
child:   	CALL   storeProcessIDs
            PUSH  [me]
            PUSH  [myParent]

        	
         	MOV   CX, [me]         	
            shmat [shmid]       // Attach to shared memory segment (DS points to it)
            log   "SHMAT RETURNS"
            log   DS
            PUSH  DS

            MOV   DI, CX        
            MUL   DI, 4         // Now DI contains 4 * pid (i.e., 8, 12, 16)

            MOV   [DI],CX       // Write pid to ES:(4*pid)
            MOV   AX, DI         
            MOV   [AX],CX       // Write pid to the shared memory segment (DS:(4*pid))
            ADD   DI, 4

            PUSH  CX
         	MUL   CX, childDelay
         	log   "I WILL EXIT AFTER THIS NUMBER OF TICKS"
         	log   CX
         	CALL  busyWait
         	POP   CX

         	CMP   CX, 4        // Process 4 will shutdown
         	JZ    shutdown
         	shmdt DS
            log   "SHMDT RETURNS"
            log   AX
            MOV   DS, 5
            MOV   DX, 0
            MOV   [DX],88      // 5:0 <-- 88 (Written to private segment)
         	exit  BX
         	log   "UNREACHABLE"    	       	
shutdown: 	shutdown    

//
//
//

storeProcessIDs:  getpid
                  log   "THIS IS PROCESS"
                  MOV   me, AX
                  log   [me]
                  getppid
                  log   "MY PARENT IS"
                  MOV   myParent, AX
                  log   [myParent]
                  RET

     
//
//
//
busyWait:    CMP  CX, 0
             JZ   busyWaitEnd
             DEC  CX
             JMP  busyWait
busyWaitEnd: RET

//
//
//
waitAndReportChildren:  wait  0
                        CMP  AX, -1
                        JZ   endReport
                        LOG  "PID OF TERMINATED CHILD"
                        LOG  AX
                        JMP  waitAndReportChildren
endReport:              RET