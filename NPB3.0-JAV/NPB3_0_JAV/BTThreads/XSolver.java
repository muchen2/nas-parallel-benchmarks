/*
!-------------------------------------------------------------------------!
!									  !
!	 N  A  S     P A R A L L E L	 B E N C H M A R K S  3.0	  !
!									  !
!			J A V A 	V E R S I O N			  !
!									  !
!                             X S o l v e r                               !
!                                                                         !
!-------------------------------------------------------------------------!
!                                                                         !
!    XSolver implements thread for x_solve subroutine                     !
!    of the BT benchmark.                                                 !
!									  !
!    Permission to use, copy, distribute and modify this software	  !
!    for any purpose with or without fee is hereby granted.  We 	  !
!    request, however, that all derived work reference the NAS  	  !
!    Parallel Benchmarks 3.0. This software is provided "as is" 	  !
!    without express or implied warranty.				  !
!									  !
!    Information on NPB 3.0, including the Technical Report NAS-02-008	  !
!    "Implementation of the NAS Parallel Benchmarks in Java",		  !
!    original specifications, source code, results and information	  !
!    on how to submit new results, is available at:			  !
!									  !
!	    http://www.nas.nasa.gov/Software/NPB/			  !
!									  !
!    Send comments or suggestions to  npb@nas.nasa.gov  		  !
!									  !
!	   NAS Parallel Benchmarks Group				  !
!	   NASA Ames Research Center					  !
!	   Mail Stop: T27A-1						  !
!	   Moffett Field, CA   94035-1000				  !
!									  !
!	   E-mail:  npb@nas.nasa.gov					  !
!	   Fax:     (650) 604-3957					  !
!									  !
!-------------------------------------------------------------------------!
!     Translation to Java and to MultiThreaded Code:			  !
!     Michael A. Frumkin					          !
!     Mathew Schultz	   					          !
!-------------------------------------------------------------------------!
*/
package NPB3_0_JAV.BTThreads;
import NPB3_0_JAV.BT;

public class XSolver extends BTBase{
  public int id;
  public boolean done = true;

  //private arrays and data
  double fjac[] = null;
  double njac[] = null;
  double lhs[] = null;
  double tmp1;
  double tmp2;
  double tmp3;
  int lower_bound;
  int upper_bound;

  public XSolver(BT bt,int low, int high){
    Init(bt);
    lower_bound=low;
    upper_bound=high;
    fjac =  new double[5*5*(problem_size+1)];
    njac =  new double[5*5*(problem_size+1)];
    lhs =  new double[5*5*3*(problem_size+1)];
    setPriority(Thread.MAX_PRIORITY);
    setDaemon(true);
    master=bt;
  }
  void Init(BT bt){
    //initialize shared data
    IMAX=bt.IMAX;
    JMAX=bt.JMAX;
    KMAX=bt.KMAX;
    problem_size=bt.problem_size;
    grid_points=bt.grid_points;
    niter_default=bt.niter_default;
    dt_default=bt.dt_default;

    u=bt.u;
    rhs=bt.rhs;
    forcing=bt.forcing;
    cv=bt.cv;
    q=bt.q;
    cuf=bt.cuf;
    isize2=bt.isize2;
    jsize2=bt.jsize2;
    ksize2=bt.ksize2;

    us=bt.us;
    vs=bt.vs;
    ws=bt.ws;
    qs=bt.qs;
    rho_i=bt.rho_i;
    square=bt.square;
    jsize1=bt.jsize1;
    ksize1=bt.ksize1;

    ue=bt.ue;
    buf=bt.buf;
    jsize3=bt.jsize3;
  }
  public void run(){
    for(;;){
      synchronized(this){
      while(done==true){
        try{
	  wait();
	  synchronized(master){ master.notify();}
        }catch(InterruptedException ie){}
      }
      step();
      synchronized(master){done=true; master.notify();}
      }
    }
  }

  public void step(){
  int i,j,k,m,n,isize;

//---------------------------------------------------------------------
//     This function computes the left hand side in the xi-direction
//---------------------------------------------------------------------

      isize = grid_points[0]-1;

//---------------------------------------------------------------------
//     determine a (labeled f) and n jacobians
//---------------------------------------------------------------------
      for(k=lower_bound;k<=upper_bound;k++){
         for(j=1;j<=grid_points[1]-2;j++){
            for(i=0;i<=isize;i++){

               tmp1 = rho_i[i+j*jsize1+k*ksize1];
               tmp2 = tmp1 * tmp1;
               tmp3 = tmp1 * tmp2;
//---------------------------------------------------------------------
//---------------------------------------------------------------------
               fjac[0+0*isize4+i*jsize4] = 0.0;
               fjac[0+1*isize4+i*jsize4] = 1.0;
               fjac[0+2*isize4+i*jsize4] = 0.0;
               fjac[0+3*isize4+i*jsize4] = 0.0;
               fjac[0+4*isize4+i*jsize4] = 0.0;

               fjac[1+0*isize4+i*jsize4] = -(u[1+i*isize2+j*jsize2+k*ksize2]) * tmp2 *
                    u[1+i*isize2+j*jsize2+k*ksize2]
                    + c2 * qs[i+j*jsize1+k*ksize1];
               fjac[1+1*isize4+i*jsize4] = ( 2.0 - c2 )
                    * ( u[1+i*isize2+j*jsize2+k*ksize2]  / u[0+i*isize2+j*jsize2+k*ksize2] );
               fjac[1+2*isize4+i*jsize4] = - c2 * ( u[2+i*isize2+j*jsize2+k*ksize2] * tmp1 );
               fjac[1+3*isize4+i*jsize4] = - c2 * ( u[3+i*isize2+j*jsize2+k*ksize2] * tmp1 );
               fjac[1+4*isize4+i*jsize4] = c2;

               fjac[2+0*isize4+i*jsize4] = - ( u[1+i*isize2+j*jsize2+k*ksize2]*u[2+i*isize2+j*jsize2+k*ksize2] ) * tmp2;
               fjac[2+1*isize4+i*jsize4] = u[2+i*isize2+j*jsize2+k*ksize2] * tmp1;
               fjac[2+2*isize4+i*jsize4] = u[1+i*isize2+j*jsize2+k*ksize2] * tmp1;
               fjac[2+3*isize4+i*jsize4] = 0.0;
               fjac[2+4*isize4+i*jsize4] = 0.0;

               fjac[3+0*isize4+i*jsize4] = - ( u[1+i*isize2+j*jsize2+k*ksize2]*u[3+i*isize2+j*jsize2+k*ksize2] ) * tmp2;
               fjac[3+1*isize4+i*jsize4] = u[3+i*isize2+j*jsize2+k*ksize2] * tmp1;
               fjac[3+2*isize4+i*jsize4] = 0.0;
               fjac[3+3*isize4+i*jsize4] = u[1+i*isize2+j*jsize2+k*ksize2] * tmp1;
               fjac[3+4*isize4+i*jsize4] = 0.0;

               fjac[4+0*isize4+i*jsize4] = ( c2 * 2.0 * square[i+j*jsize1+k*ksize1]
                    - c1 * u[4+i*isize2+j*jsize2+k*ksize2] )
                    * ( u[1+i*isize2+j*jsize2+k*ksize2] * tmp2 );
               fjac[4+1*isize4+i*jsize4] = c1 *  u[4+i*isize2+j*jsize2+k*ksize2] * tmp1
                    - c2
                    * ( u[1+i*isize2+j*jsize2+k*ksize2]*u[1+i*isize2+j*jsize2+k*ksize2] * tmp2
                    + qs[i+j*jsize1+k*ksize1]);
               fjac[4+2*isize4+i*jsize4] = - c2 * ( u[2+i*isize2+j*jsize2+k*ksize2]*u[1+i*isize2+j*jsize2+k*ksize2] )
                    * tmp2;
               fjac[4+3*isize4+i*jsize4] = - c2 * ( u[3+i*isize2+j*jsize2+k*ksize2]*u[1+i*isize2+j*jsize2+k*ksize2] )
                    * tmp2;
               fjac[4+4*isize4+i*jsize4] = c1 * ( u[1+i*isize2+j*jsize2+k*ksize2] * tmp1 );

               njac[0+0*isize4+i*jsize4] = 0.0;
               njac[0+1*isize4+i*jsize4] = 0.0;
               njac[0+2*isize4+i*jsize4] = 0.0;
               njac[0+3*isize4+i*jsize4] = 0.0;
               njac[0+4*isize4+i*jsize4] = 0.0;

               njac[1+0*isize4+i*jsize4] = - con43 * c3c4 * tmp2 * u[1+i*isize2+j*jsize2+k*ksize2];
               njac[1+1*isize4+i*jsize4] =   con43 * c3c4 * tmp1;
               njac[1+2*isize4+i*jsize4] =   0.0;
               njac[1+3*isize4+i*jsize4] =   0.0;
               njac[1+4*isize4+i*jsize4] =   0.0;

               njac[2+0*isize4+i*jsize4] = - c3c4 * tmp2 * u[2+i*isize2+j*jsize2+k*ksize2];
               njac[2+1*isize4+i*jsize4] =   0.0;
               njac[2+2*isize4+i*jsize4] =   c3c4 * tmp1;
               njac[2+3*isize4+i*jsize4] =   0.0;
               njac[2+4*isize4+i*jsize4] =   0.0;

               njac[3+0*isize4+i*jsize4] = - c3c4 * tmp2 * u[3+i*isize2+j*jsize2+k*ksize2];
               njac[3+1*isize4+i*jsize4] =   0.0 ;
               njac[3+2*isize4+i*jsize4] =   0.0;
               njac[3+3*isize4+i*jsize4] =   c3c4 * tmp1;
               njac[3+4*isize4+i*jsize4] =   0.0;

               njac[4+0*isize4+i*jsize4] = - ( con43 * c3c4
                    - c1345 ) * tmp3 * ( Math.pow(u[1+i*isize2+j*jsize2+k*ksize2],2) )
                    - ( c3c4 - c1345 ) * tmp3 * ( Math.pow(u[2+i*isize2+j*jsize2+k*ksize2],2) )
                    - ( c3c4 - c1345 ) * tmp3 * ( Math.pow(u[3+i*isize2+j*jsize2+k*ksize2],2) )
                    - c1345 * tmp2 * u[4+i*isize2+j*jsize2+k*ksize2];

               njac[4+1*isize4+i*jsize4] = ( con43 * c3c4
                    - c1345 ) * tmp2 * u[1+i*isize2+j*jsize2+k*ksize2];
               njac[4+2*isize4+i*jsize4] = ( c3c4 - c1345 ) * tmp2 * u[2+i*isize2+j*jsize2+k*ksize2];
               njac[4+3*isize4+i*jsize4] = ( c3c4 - c1345 ) * tmp2 * u[3+i*isize2+j*jsize2+k*ksize2];
               njac[4+4*isize4+i*jsize4] = ( c1345 ) * tmp1;

            }
//---------------------------------------------------------------------
//     now jacobians set, so form left hand side in x direction
//---------------------------------------------------------------------
            lhsinit(lhs, isize);
	
            for(i=1;i<=isize-1;i++){

               tmp1 = dt * tx1;
               tmp2 = dt * tx2;

               lhs[0+0*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[0+0*isize4+(i-1)*jsize4]
                    - tmp1 * njac[0+0*isize4+(i-1)*jsize4]
                    - tmp1 * dx1 ;
               lhs[0+1*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[0+1*isize4+(i-1)*jsize4]
                    - tmp1 * njac[0+1*isize4+(i-1)*jsize4];
               lhs[0+2*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[0+2*isize4+(i-1)*jsize4]
                    - tmp1 * njac[0+2*isize4+(i-1)*jsize4];
               lhs[0+3*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[0+3*isize4+(i-1)*jsize4]
                    - tmp1 * njac[0+3*isize4+(i-1)*jsize4];
               lhs[0+4*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[0+4*isize4+(i-1)*jsize4]
                    - tmp1 * njac[0+4*isize4+(i-1)*jsize4];

               lhs[1+0*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[1+0*isize4+(i-1)*jsize4]
                    - tmp1 * njac[1+0*isize4+(i-1)*jsize4];
               lhs[1+1*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[1+1*isize4+(i-1)*jsize4]
                    - tmp1 * njac[1+1*isize4+(i-1)*jsize4]
                    - tmp1 * dx2;
               lhs[1+2*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[1+2*isize4+(i-1)*jsize4]
                    - tmp1 * njac[1+2*isize4+(i-1)*jsize4];
               lhs[1+3*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[1+3*isize4+(i-1)*jsize4]
                    - tmp1 * njac[1+3*isize4+(i-1)*jsize4];
               lhs[1+4*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[1+4*isize4+(i-1)*jsize4]
                    - tmp1 * njac[1+4*isize4+(i-1)*jsize4];

               lhs[2+0*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[2+0*isize4+(i-1)*jsize4]
                    - tmp1 * njac[2+0*isize4+(i-1)*jsize4];
               lhs[2+1*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[2+1*isize4+(i-1)*jsize4]
                    - tmp1 * njac[2+1*isize4+(i-1)*jsize4];
               lhs[2+2*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[2+2*isize4+(i-1)*jsize4]
                    - tmp1 * njac[2+2*isize4+(i-1)*jsize4]
                    - tmp1 * dx3 ;
               lhs[2+3*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[2+3*isize4+(i-1)*jsize4]
                    - tmp1 * njac[2+3*isize4+(i-1)*jsize4];
               lhs[2+4*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[2+4*isize4+(i-1)*jsize4]
                    - tmp1 * njac[2+4*isize4+(i-1)*jsize4];

               lhs[3+0*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[3+0*isize4+(i-1)*jsize4]
                    - tmp1 * njac[3+0*isize4+(i-1)*jsize4];
               lhs[3+1*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[3+1*isize4+(i-1)*jsize4]
                    - tmp1 * njac[3+1*isize4+(i-1)*jsize4];
               lhs[3+2*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[3+2*isize4+(i-1)*jsize4]
                    - tmp1 * njac[3+2*isize4+(i-1)*jsize4];
               lhs[3+3*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[3+3*isize4+(i-1)*jsize4]
                    - tmp1 * njac[3+3*isize4+(i-1)*jsize4]
                    - tmp1 * dx4;
               lhs[3+4*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[3+4*isize4+(i-1)*jsize4]
                    - tmp1 * njac[3+4*isize4+(i-1)*jsize4];

               lhs[4+0*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[4+0*isize4+(i-1)*jsize4]
                    - tmp1 * njac[4+0*isize4+(i-1)*jsize4];
               lhs[4+1*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[4+1*isize4+(i-1)*jsize4]
                    - tmp1 * njac[4+1*isize4+(i-1)*jsize4];
               lhs[4+2*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[4+2*isize4+(i-1)*jsize4]
                    - tmp1 * njac[4+2*isize4+(i-1)*jsize4];
               lhs[4+3*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[4+3*isize4+(i-1)*jsize4]
                    - tmp1 * njac[4+3*isize4+(i-1)*jsize4];
               lhs[4+4*isize4+aa*jsize4+i*ksize4] = - tmp2 * fjac[4+4*isize4+(i-1)*jsize4]
                    - tmp1 * njac[4+4*isize4+(i-1)*jsize4]
                    - tmp1 * dx5;
		    		
               lhs[0+0*isize4+bb*jsize4+i*ksize4] = 1.0
                    + tmp1 * 2.0 * njac[0+0*isize4+i*jsize4]
                    + tmp1 * 2.0 * dx1;
               lhs[0+1*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[0+1*isize4+i*jsize4];
               lhs[0+2*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[0+2*isize4+i*jsize4];
               lhs[0+3*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[0+3*isize4+i*jsize4];
               lhs[0+4*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[0+4*isize4+i*jsize4];

	       lhs[1+0*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[1+0*isize4+i*jsize4];
               lhs[1+1*isize4+bb*jsize4+i*ksize4] = 1.0
                    + tmp1 * 2.0 * njac[1+1*isize4+i*jsize4]
                    + tmp1 * 2.0 * dx2;
               lhs[1+2*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[1+2*isize4+i*jsize4];
               lhs[1+3*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[1+3*isize4+i*jsize4];
               lhs[1+4*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[1+4*isize4+i*jsize4];

               lhs[2+0*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[2+0*isize4+i*jsize4];
               lhs[2+1*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[2+1*isize4+i*jsize4];
               lhs[2+2*isize4+bb*jsize4+i*ksize4] = 1.0
                    + tmp1 * 2.0 * njac[2+2*isize4+i*jsize4]
                    + tmp1 * 2.0 * dx3;
               lhs[2+3*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[2+3*isize4+i*jsize4];
               lhs[2+4*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[2+4*isize4+i*jsize4];

               lhs[3+0*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[3+0*isize4+i*jsize4];
               lhs[3+1*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[3+1*isize4+i*jsize4];
               lhs[3+2*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[3+2*isize4+i*jsize4];
               lhs[3+3*isize4+bb*jsize4+i*ksize4] = 1.0
                    + tmp1 * 2.0 * njac[3+3*isize4+i*jsize4]
                    + tmp1 * 2.0 * dx4;
               lhs[3+4*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[3+4*isize4+i*jsize4];

               lhs[4+0*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[4+0*isize4+i*jsize4];
               lhs[4+1*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[4+1*isize4+i*jsize4];
               lhs[4+2*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[4+2*isize4+i*jsize4];
               lhs[4+3*isize4+bb*jsize4+i*ksize4] = tmp1 * 2.0 * njac[4+3*isize4+i*jsize4];
               lhs[4+4*isize4+bb*jsize4+i*ksize4] = 1.0
                    + tmp1 * 2.0 * njac[4+4*isize4+i*jsize4]
                    + tmp1 * 2.0 * dx5;
	
	       lhs[0+0*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[0+0*isize4+(i+1)*jsize4]
                    - tmp1 * njac[0+0*isize4+(i+1)*jsize4]
                    - tmp1 * dx1;
               lhs[0+1*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[0+1*isize4+(i+1)*jsize4]
                    - tmp1 * njac[0+1*isize4+(i+1)*jsize4];
               lhs[0+2*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[0+2*isize4+(i+1)*jsize4]
                    - tmp1 * njac[0+2*isize4+(i+1)*jsize4];
               lhs[0+3*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[0+3*isize4+(i+1)*jsize4]
                    - tmp1 * njac[0+3*isize4+(i+1)*jsize4];
               lhs[0+4*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[0+4*isize4+(i+1)*jsize4]
                    - tmp1 * njac[0+4*isize4+(i+1)*jsize4];

               lhs[1+0*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[1+0*isize4+(i+1)*jsize4]
                    - tmp1 * njac[1+0*isize4+(i+1)*jsize4];
               lhs[1+1*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[1+1*isize4+(i+1)*jsize4]
                    - tmp1 * njac[1+1*isize4+(i+1)*jsize4]
                    - tmp1 * dx2;
               lhs[1+2*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[1+2*isize4+(i+1)*jsize4]
                    - tmp1 * njac[1+2*isize4+(i+1)*jsize4];
               lhs[1+3*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[1+3*isize4+(i+1)*jsize4]
                    - tmp1 * njac[1+3*isize4+(i+1)*jsize4];
               lhs[1+4*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[1+4*isize4+(i+1)*jsize4]
                    - tmp1 * njac[1+4*isize4+(i+1)*jsize4];

               lhs[2+0*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[2+0*isize4+(i+1)*jsize4]
                    - tmp1 * njac[2+0*isize4+(i+1)*jsize4];
               lhs[2+1*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[2+1*isize4+(i+1)*jsize4]
                    - tmp1 * njac[2+1*isize4+(i+1)*jsize4];
               lhs[2+2*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[2+2*isize4+(i+1)*jsize4]
                    - tmp1 * njac[2+2*isize4+(i+1)*jsize4]
                    - tmp1 * dx3;
               lhs[2+3*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[2+3*isize4+(i+1)*jsize4]
                    - tmp1 * njac[2+3*isize4+(i+1)*jsize4];
               lhs[2+4*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[2+4*isize4+(i+1)*jsize4]
                    - tmp1 * njac[2+4*isize4+(i+1)*jsize4];

               lhs[3+0*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[3+0*isize4+(i+1)*jsize4]
                    - tmp1 * njac[3+0*isize4+(i+1)*jsize4];
               lhs[3+1*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[3+1*isize4+(i+1)*jsize4]
                    - tmp1 * njac[3+1*isize4+(i+1)*jsize4];
               lhs[3+2*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[3+2*isize4+(i+1)*jsize4]
                    - tmp1 * njac[3+2*isize4+(i+1)*jsize4];
               lhs[3+3*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[3+3*isize4+(i+1)*jsize4]
                    - tmp1 * njac[3+3*isize4+(i+1)*jsize4]
                    - tmp1 * dx4;
               lhs[3+4*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[3+4*isize4+(i+1)*jsize4]
                    - tmp1 * njac[3+4*isize4+(i+1)*jsize4];

               lhs[4+0*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[4+0*isize4+(i+1)*jsize4]
                    - tmp1 * njac[4+0*isize4+(i+1)*jsize4];
               lhs[4+1*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[4+1*isize4+(i+1)*jsize4]
                    - tmp1 * njac[4+1*isize4+(i+1)*jsize4];
               lhs[4+2*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[4+2*isize4+(i+1)*jsize4]
                    - tmp1 * njac[4+2*isize4+(i+1)*jsize4];
               lhs[4+3*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[4+3*isize4+(i+1)*jsize4]
                    - tmp1 * njac[4+3*isize4+(i+1)*jsize4];
               lhs[4+4*isize4+cc*jsize4+i*ksize4] =  tmp2 * fjac[4+4*isize4+(i+1)*jsize4]
                    - tmp1 * njac[4+4*isize4+(i+1)*jsize4]
                    - tmp1 * dx5;

            }
	
//---------------------------------------------------------------------
//     performs guaussian elimination on this cell.
//
//     assumes that unpacking routines for non-first cells
//     preload C' and rhs' from previous cell.
//
//     assumed send happens outside this routine, but that
//     c'(IMAX) and rhs'(IMAX) will be sent to next cell
//---------------------------------------------------------------------

//---------------------------------------------------------------------
//     outer most do loops - sweeping in i direction
//---------------------------------------------------------------------
	
//---------------------------------------------------------------------
//     multiply c(0,j,k) by b_inverse and copy back to c
//     multiply rhs(0) by b_inverse(0) and copy to rhs
//---------------------------------------------------------------------
             binvcrhs( lhs,0+0*isize4+bb*jsize4+0*ksize4,
                              lhs,0+0*isize4+cc*jsize4+0*ksize4,
                              rhs, 0+0*isize2+j*jsize2+k*ksize2);
	
//---------------------------------------------------------------------
//     begin inner most do loop
//     do all the elements of the cell unless last
//---------------------------------------------------------------------
            for(i=1;i<=isize-1;i++){

//---------------------------------------------------------------------
//     rhs(i) = rhs(i) - A*rhs(i-1)
//---------------------------------------------------------------------
                matvec_sub(lhs,0+0*isize4+aa*jsize4+i*ksize4,
                               rhs,0+(i-1)*isize2+j*jsize2+k*ksize2,
	                       rhs,0+i*isize2+j*jsize2+k*ksize2);

//---------------------------------------------------------------------
//     B(i) = B(i) - C(i-1)*A(i)
//---------------------------------------------------------------------
                matmul_sub(lhs,0+0*isize4+aa*jsize4+i*ksize4,
                               lhs,0+0*isize4+cc*jsize4+(i-1)*ksize4,
                               lhs,0+0*isize4+bb*jsize4+i*ksize4);
//---------------------------------------------------------------------
//     multiply c(i,j,k) by b_inverse and copy back to c
//     multiply rhs(1,j,k) by b_inverse(1,j,k) and copy to rhs
//---------------------------------------------------------------------
                binvcrhs( lhs,0+0*isize4+bb*jsize4+i*ksize4,
                              lhs,0+0*isize4+cc*jsize4+i*ksize4,
                              rhs,0+i*isize2+j*jsize2+k*ksize2 );
     }

//---------------------------------------------------------------------
//     rhs(isize) = rhs(isize) - A*rhs(isize-1)
//---------------------------------------------------------------------
             matvec_sub(lhs,0+0*isize4+aa*jsize4+isize*ksize4,
			    rhs,0+(isize-1)*isize2+j*jsize2+k*ksize2,
			    rhs,0+isize*isize2+j*jsize2+k*ksize2);

//---------------------------------------------------------------------
//     B(isize) = B(isize) - C(isize-1)*A(isize)
//---------------------------------------------------------------------
             matmul_sub(lhs,0+0*isize4+aa*jsize4+isize*ksize4,
                               lhs,0+0*isize4+cc*jsize4+(isize-1)*ksize4,
                               lhs,0+0*isize4+bb*jsize4+isize*ksize4);

//---------------------------------------------------------------------
//     multiply rhs() by b_inverse() and copy to rhs
//---------------------------------------------------------------------
             binvrhs( lhs,0+0*isize4+bb*jsize4+isize*ksize4,
                             rhs,0+isize*isize2+j*jsize2+k*ksize2);

//---------------------------------------------------------------------
//     back solve: if last cell, then generate U(isize)=rhs(isize)
//     else assume U(isize) is loaded in un pack backsub_info
//     so just use it
//     after call u(istart) will be sent to next cell
//---------------------------------------------------------------------

            for(i=isize-1;i>=0;i--){
               for(m=0;m<=BLOCK_SIZE-1;m++){
                  for(n=0;n<=BLOCK_SIZE-1;n++){
                     rhs[m+i*isize2+j*jsize2+k*ksize2] -=
                          lhs[m+n*isize4+cc*jsize4+i*ksize4]*rhs[n+(i+1)*isize2+j*jsize2+k*ksize2];
                  }
               }
            }
         }
      }
  }
}

