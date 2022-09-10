class A{ 

    protected int f = 10;
}


class B{ 


    public void action(){
    }
}


class Main extends A{ 

    protected int x = 0;
    protected int y = 1;
    protected A a = this;

    public int returnNumber(int x, int y){
        while(x == 0){
            x = 1;
        }
        if(x < 10){
            x = this.x;
            y = super.f;
        }
        return x + y;
    }

    public String returnString(){
        var s = "Hello";
        returnNumber(2, 3);
        return s;
    }

    public static void main(String[] args){
        var main = new Main();
        var i = 0;
        for(i = 0; i < 2; i++){
            var j = 0;
            System.out.println("");
            System.out.println("Going to sleep");
            for(j = 0; j < 5; j++){
                System.out.println("Zzz...");
            }
            System.out.println("Waking up!");
            var k = 0;
            while(k < 5){
                System.out.println("Going to class");
                k++;
            }
        }
    }
}


