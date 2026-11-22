"""Author one-shot opening GIFs from the existing reward artwork, without redesigning it."""
from pathlib import Path
from math import sin, pi
from PIL import Image, ImageDraw, ImageChops, ImageFilter

ROOT=Path(__file__).resolve().parents[1]
ART=ROOT/'app/src/main/res/drawable-nodpi'
RAW=ROOT/'app/src/main/res/raw'
SIZE=1280

def read(name):
    return Image.open(ART/(name+'.png')).convert('RGBA').resize((SIZE,SIZE),Image.Resampling.LANCZOS)

def place(image,angle=0,dy=0):
    image=image.resize((1000,1000),Image.Resampling.LANCZOS)
    if angle:image=image.rotate(angle,Image.Resampling.BICUBIC,expand=False)
    canvas=Image.new('RGBA',(SIZE,SIZE));canvas.alpha_composite(image,(140,180+round(dy)))
    return canvas

def gift_frame(source,progress):
    if progress==0:return place(source)
    mask=Image.new('L',source.size)
    ImageDraw.Draw(mask).polygon([(0,0),(1280,0),(1280,550),(1105,622),(686,792),(140,638),(0,588)],fill=255)
    lid=source.copy();lid.putalpha(ImageChops.multiply(source.getchannel('A'),mask))
    body=source.copy();body.putalpha(ImageChops.multiply(source.getchannel('A'),ImageChops.invert(mask)))
    interior=Image.new('RGBA',source.size);d=ImageDraw.Draw(interior)
    d.polygon([(150,636),(570,470),(1094,620),(686,788)],fill=(105,49,14,255))
    d.line([(150,636),(570,470),(1094,620),(686,788),(150,636)],fill=(255,202,45,255),width=18)
    interior.alpha_composite(body)
    canvas=place(interior)
    canvas.alpha_composite(place(lid,angle=progress*9,dy=-progress*230))
    return canvas

def save(name,frames):
    # One shared palette, with a reserved transparent index, avoids flashes between frames.
    sample=Image.new('RGB',(640*3,640),(255,246,217))
    for i,frame in enumerate([frames[0],frames[len(frames)//2],frames[-1]]):
        small=frame.resize((640,640),Image.Resampling.LANCZOS)
        sample.paste(small,(640*i,0),small)
    palette=sample.quantize(colors=255)
    palette.putpalette(palette.getpalette()[:765]+[0,0,0])
    indexed=[]
    for frame in frames:
        small=frame.resize((640,640),Image.Resampling.LANCZOS)
        rgb=Image.new('RGB',small.size,(255,224,140));rgb.paste(small,mask=small.getchannel('A'))
        result=rgb.quantize(palette=palette,dither=Image.Dither.NONE)
        result.paste(255,mask=small.getchannel('A').point(lambda a:255 if a<100 else 0))
        indexed.append(result)
    indexed[0].save(RAW/(name+'_opening.gif'),save_all=True,append_images=indexed[1:],
                    duration=60,transparency=255,disposal=2,optimize=False)
    frames[-1].save(ART/(name+'_opened_original.png'))
    frames[0].save(ART/(name+'_closed_original.png'))
    print(name,len(frames),'frames,',len(frames)*60,'ms',flush=True)

if __name__=='__main__':
    gift=read('item_gift_hq');closed=read('item_chest_closed_hq');opened=read('item_chest_open_hq')
    gift_frames=[];chest_frames=[]
    for index in range(30):
        t=index/29
        p=max(0,min(1,(t-.22)/.58));ease=1-(1-p)**3
        shake=sin(t*pi*22)*3*(1-t/.25) if t<.25 else 0
        g=gift_frame(gift,ease)
        if shake:g=g.rotate(shake,Image.Resampling.BICUBIC)
        gift_frames.append(g)
        # The existing open chest is the end pose. Lift the lid silhouette into that pose.
        c=place(closed,angle=shake)
        if p>0:
            mask=Image.new('L',closed.size)
            ImageDraw.Draw(mask).polygon([(0,0),(1280,0),(1280,525),(1180,579),(836,705),(80,566),(0,550)],fill=255)
            lid=closed.copy();lid.putalpha(ImageChops.multiply(closed.getchannel('A'),mask))
            body=closed.copy();body.putalpha(ImageChops.multiply(closed.getchannel('A'),ImageChops.invert(mask)))
            lifted=place(body);lifted.alpha_composite(place(lid,angle=ease*12,dy=-ease*160))
            c=lifted if p<.42 else place(opened)
            # A short golden reveal hides the change of perspective without ghosted double lids.
            flash=max(0,1-abs(p-.42)/.24)
            if flash:
                light=Image.new('RGBA',c.size)
                ImageDraw.Draw(light).ellipse((180,60,1130,1100),fill=(255,243,165,round(245*flash)))
                c.alpha_composite(light.filter(ImageFilter.GaussianBlur(65)))
        chest_frames.append(c)
    save('gift',gift_frames);save('chest',chest_frames)
