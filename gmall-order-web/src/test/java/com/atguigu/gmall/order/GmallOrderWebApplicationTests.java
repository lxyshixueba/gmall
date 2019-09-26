package com.atguigu.gmall.order;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallOrderWebApplicationTests {

    int arr[] = new int[]{45,21,33,8,2456,46};

//    public static void main(String[] args) {
//        insertSort(arr);
//        System.out.println(Arrays.toString(arr));
//    }

    @Test
    public void contextLoads() {
        insertSort(arr);
        System.out.println(Arrays.toString(arr));
    }

    public static void insertSort(int arr[]){
        for (int i = 1; i < arr.length; i++) {
            int insertIndex = i-1;//0
            int insertValue = arr[i];//21
            //insertValue=arr[i]
            //arr[insertIndex]=arr[i-1]
            while(insertIndex>=0&&insertValue<arr[insertIndex]){//21<arr[0]
//                //TODO 在这里 假如 插入的值比 第一个值小的话 插入的值就只能排在 原有值 后面 那么我应该将插入的值放哪里呢
                arr[insertIndex+1] = arr[insertIndex];
                insertIndex--;
            }
            arr[insertIndex+1] = insertValue;

//            while(insertIndex>=0&&insertValue<arr[insertIndex]){//21<arr[0]
//                //TODO 在这里 假如 插入的值比 第一个值小的话 插入的值就只能排在 原有值 后面 那么我应该将插入的值放哪里呢
//                arr[insertIndex+1] = arr[insertIndex];
//                insertIndex--;
//            }
//            arr[insertIndex+1] = insertValue;
        }
    }
}
