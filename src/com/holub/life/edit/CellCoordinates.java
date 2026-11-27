package com.holub.life.edit;

import java.awt.Point;

/**
 * 화면의 픽셀 좌표를 셀 좌표로 바꾼다. 화면(Universe)과 편집 도구
 * 사이의 유일한 좌표 변환 지점이다.
 */
public final class CellCoordinates
{
	private CellCoordinates() { }

	/**
	 * @param pixel        보드 왼쪽 위를 (0,0)으로 한 픽셀 위치.
	 *                     끌다가 보드 밖으로 나가면 음수이거나 보드 크기보다 클 수 있다.
	 * @param boardPixels  보드 한 변의 픽셀 수
	 * @param cellsPerSide 보드 한 변의 셀 수
	 * @return 셀 좌표 (x = column, y = row). 보드 밖 픽셀은 보드 밖 셀 좌표가 된다.
	 */
	public static Point fromPixel( Point pixel, int boardPixels, int cellsPerSide )
	{
		int pixelsPerCell = boardPixels / cellsPerSide;
		if( pixelsPerCell <= 0 )				// 창이 너무 작아 셀을 그릴 수 없는 상태
			return new Point( -1, -1 );

		// 나눗셈은 0 쪽으로 버리므로 -3/8 이 0 이 되어 보드 밖 픽셀이
		// 0번 칸으로 잘못 들어온다. floorDiv 는 -1 을 돌려준다.
		return new Point( Math.floorDiv(pixel.x, pixelsPerCell),
						  Math.floorDiv(pixel.y, pixelsPerCell) );
	}
}
