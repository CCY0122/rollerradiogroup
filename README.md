# rollerradiogroup
Android横向滚动单选控件

## 效果图
![image](https://github.com/CCY0122/rollerradiogroup/blob/master/device-2017-12-02-170414.gif)

## 用法:
```
  <ccy.rollerradiogroup.RollerRadioGroup
      android:id="@+id/r1"
      android:layout_width="280dp"
      android:layout_height="40dp"
      android:layout_margin="20dp"
      //部分属性
      app:normal_size="15sp"
      app:selected_size="22sp"
      app:show_edge_line="false"
      /> 
```

```
 List<String> data = new ArrayList<>();
        data.add("你好");
        data.add("陈朝勇");
        data.add("English");
        data.add("名字要长长长长");
        data.add("1234");
        //……………………
        
 RollerRadioGroup r1 = (RollerRadioGroup) findViewById(R.id.r1);
 
  r1.setNormalColor(0xff787878);
  r1.setAutoSelected(true);
  //…………省略各种r1.setXXX
   r1.setData(data);
  
```
