package com.rin23.app;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
/** Animated by the loader only while the owning view is visible. */
final class LoadingPicture extends Drawable {
 private final Paint paint=new Paint(3);private final Drawable logo;private final boolean light;private final String message;private final boolean moving;
 LoadingPicture(android.content.Context c,String text,boolean animate){logo=c.getDrawable(R.drawable.rin_loading).mutate();message=text;moving=animate;light=(c.getResources().getConfiguration().uiMode&48)==16;}
 @Override public int getIntrinsicWidth(){return 640;}
 @Override public int getIntrinsicHeight(){return 360;}
 public void draw(Canvas canvas){paint.reset();paint.setAntiAlias(true);Rect b=getBounds();float w=b.width(),h=b.height(),cx=b.exactCenterX(),cy=b.exactCenterY();paint.setColor(light?0xffe9e9ec:0xff1c1d23);canvas.drawRect(b,paint);
  float phase=(SystemClock.uptimeMillis()%1600)/1600f;
  if(moving){float x=b.left+phase*w*2-w/2;paint.setShader(new LinearGradient(x-w/3,0,x+w/3,0,new int[]{0x00ffffff,0x18ffffff,0x00ffffff},null,Shader.TileMode.CLAMP));canvas.drawRect(b,paint);paint.setShader(null);}
  int size=(int)Math.min(w*.22f,h*.36f);logo.setBounds((int)cx-size/2,(int)(cy-h*.09f)-size/2,(int)cx+size/2,(int)(cy-h*.09f)+size/2);logo.draw(canvas);
  if(moving){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Math.max(2,w/180));paint.setStrokeCap(Paint.Cap.ROUND);paint.setColor(0xffff6961);float radius=size*.68f;canvas.drawArc(cx-radius,cy-h*.09f-radius,cx+radius,cy-h*.09f+radius,phase*360,85,false,paint);paint.setStyle(Paint.Style.FILL);}
  paint.setColor(light?0xff555560:0xffbbbbc3);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(Math.min(h*.09f,w*.043f));canvas.drawText(message,cx,cy+h*.31f,paint);
 }
 public void setAlpha(int a){paint.setAlpha(a);}public void setColorFilter(ColorFilter f){paint.setColorFilter(f);}public int getOpacity(){return PixelFormat.OPAQUE;}
}
